package com.qinyadan.system.dsp.engine.operator;

import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.value.Value;
import com.qinyadan.system.dsp.engine.data.SlothRow;
import com.qinyadan.system.dsp.engine.data.expr.Symbol;
import com.qinyadan.system.dsp.engine.rex.RexToSymbolShuttle;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * One-side-materialized join executor. Direct equi-join conjuncts use a hash
 * index; other predicates retain a bounded nested-loop fallback without
 * materializing the probe side.
 */
public class SlothJoinOperator extends AbstractOperator<SlothRow> {

    private final Operator<SlothRow> left;
    private final Operator<SlothRow> right;
    private final RexNode joinCondition;
    private final JoinRelType joinType;

    private Symbol joinConditionSymbol;
    private List<DataType> leftTypes;
    private List<DataType> rightTypes;
    private List<KeyPair> hashKeys;

    private final List<SlothRow> buildRows = new ArrayList<>();
    private final Map<JoinKey, List<SlothRow>> hashIndex = new HashMap<>();
    private Operator<SlothRow> probeInput;
    private boolean buildLeft;
    private boolean initialized;

    private SlothRow probeRow;
    private List<SlothRow> candidates = Collections.emptyList();
    private int candidateIndex;
    private boolean probeMatched;

    public SlothJoinOperator(Operator<SlothRow> left, Operator<SlothRow> right,
                             RexNode joinCondition, JoinRelType joinType,
                             RelDataType rowType) {
        super(rowType);
        this.left = left;
        this.right = right;
        this.joinCondition = joinCondition;
        this.joinType = joinType;
    }

    @Override
    public void open() {
        buildRows.clear();
        hashIndex.clear();
        candidates = Collections.emptyList();
        candidateIndex = 0;
        probeRow = null;
        probeMatched = false;
        initialized = false;
        left.open();
        right.open();

        leftTypes = left.getRowType();
        rightTypes = right.getRowType();
        joinConditionSymbol = joinCondition.accept(RexToSymbolShuttle.INSTANCE);
        if (joinType != JoinRelType.INNER && joinType != JoinRelType.LEFT
                && joinType != JoinRelType.RIGHT) {
            throw new UnsupportedOperationException("Unsupported join type: " + joinType);
        }
        hashKeys = extractHashKeys(joinCondition);
        buildLeft = joinType == JoinRelType.RIGHT;
        probeInput = buildLeft ? right : left;
    }

    @Override
    public SlothRow next() {
        if (!initialized) {
            initializeBuildSide();
        }

        while (true) {
            while (candidateIndex < candidates.size()) {
                SlothRow buildRow = candidates.get(candidateIndex++);
                List<Value> merged = buildLeft
                        ? merge(buildRow, probeRow) : merge(probeRow, buildRow);
                if (matches(merged)) {
                    probeMatched = true;
                    return new SlothRow(copy(merged));
                }
            }

            if (probeRow != null && !probeMatched && joinType != JoinRelType.INNER) {
                SlothRow unmatched = buildLeft
                        ? mergeWithNullLeft(probeRow) : mergeWithNullRight(probeRow);
                probeRow = null;
                return unmatched;
            }

            probeRow = probeInput.next();
            if (probeRow == SlothRow.EOF_ROW) {
                return SlothRow.EOF_ROW;
            }
            candidates = candidatesFor(probeRow);
            candidateIndex = 0;
            probeMatched = false;
        }
    }

    private void initializeBuildSide() {
        Operator<SlothRow> buildInput = buildLeft ? left : right;
        SlothRow row;
        while ((row = buildInput.next()) != SlothRow.EOF_ROW) {
            buildRows.add(row);
            QueryResourceLimits.checkMaterializedRows(buildRows.size(), "join build side");
            if (!hashKeys.isEmpty()) {
                JoinKey key = joinKey(row, true);
                if (key != null) {
                    hashIndex.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
                }
            }
        }
        initialized = true;
    }

    private List<SlothRow> candidatesFor(SlothRow row) {
        if (hashKeys.isEmpty()) {
            return buildRows;
        }
        JoinKey key = joinKey(row, false);
        if (key == null) {
            return Collections.emptyList();
        }
        List<SlothRow> matches = hashIndex.get(key);
        return matches == null ? Collections.emptyList() : matches;
    }

    private JoinKey joinKey(SlothRow row, boolean buildSide) {
        List<Value> values = new ArrayList<>(hashKeys.size());
        for (KeyPair pair : hashKeys) {
            int index;
            if (buildSide) {
                index = buildLeft ? pair.leftIndex : pair.rightIndex;
            } else {
                index = buildLeft ? pair.rightIndex : pair.leftIndex;
            }
            Value value = row.getColumn(index);
            // SQL equality never matches NULL, including NULL = NULL.
            if (value.isNull()) {
                return null;
            }
            values.add(value);
        }
        return new JoinKey(values);
    }

    private List<KeyPair> extractHashKeys(RexNode condition) {
        List<KeyPair> result = new ArrayList<>();
        collectHashKeys(condition, result);
        return result;
    }

    private void collectHashKeys(RexNode condition, List<KeyPair> result) {
        if (!(condition instanceof RexCall)) {
            return;
        }
        RexCall call = (RexCall) condition;
        if (call.getKind() == SqlKind.AND) {
            for (RexNode operand : call.getOperands()) {
                collectHashKeys(operand, result);
            }
            return;
        }
        if (call.getKind() != SqlKind.EQUALS || call.getOperands().size() != 2
                || !(call.getOperands().get(0) instanceof RexInputRef)
                || !(call.getOperands().get(1) instanceof RexInputRef)) {
            return;
        }

        int first = ((RexInputRef) call.getOperands().get(0)).getIndex();
        int second = ((RexInputRef) call.getOperands().get(1)).getIndex();
        int leftWidth = leftTypes.size();
        int leftIndex;
        int rightIndex;
        if (first < leftWidth && second >= leftWidth) {
            leftIndex = first;
            rightIndex = second - leftWidth;
        } else if (second < leftWidth && first >= leftWidth) {
            leftIndex = second;
            rightIndex = first - leftWidth;
        } else {
            return;
        }

        // Value equality includes the declared type. Only hash when both sides
        // have identical runtime types; the full predicate still verifies every candidate.
        if (leftTypes.get(leftIndex).equals(rightTypes.get(rightIndex))) {
            result.add(new KeyPair(leftIndex, rightIndex));
        }
    }

    private boolean matches(List<Value> values) {
        joinConditionSymbol.setInput(values);
        return Boolean.TRUE.equals(joinConditionSymbol.compute().booleanValue());
    }

    private List<Value> merge(SlothRow leftRow, SlothRow rightRow) {
        List<Value> values = new ArrayList<>(leftRow.columnSize() + rightRow.columnSize());
        values.addAll(leftRow.getAllColumn());
        values.addAll(rightRow.getAllColumn());
        return values;
    }

    private SlothRow mergeWithNullLeft(SlothRow rightRow) {
        List<Value> values = nullValues(leftTypes);
        values.addAll(rightRow.getAllColumn());
        return new SlothRow(copy(values));
    }

    private SlothRow mergeWithNullRight(SlothRow leftRow) {
        List<Value> values = new ArrayList<>(leftRow.getAllColumn());
        values.addAll(nullValues(rightTypes));
        return new SlothRow(copy(values));
    }

    private List<Value> nullValues(List<DataType> types) {
        List<Value> values = new ArrayList<>(types.size());
        for (DataType type : types) {
            values.add(Value.nullValue(type));
        }
        return values;
    }

    private List<Value> copy(List<Value> values) {
        List<Value> result = new ArrayList<>(values.size());
        for (Value value : values) {
            result.add(value.copy());
        }
        return result;
    }

    @Override
    public void close() {
        left.close();
        right.close();
        buildRows.clear();
        hashIndex.clear();
        candidates = Collections.emptyList();
        probeRow = null;
        initialized = false;
    }

    private static final class KeyPair {
        private final int leftIndex;
        private final int rightIndex;

        private KeyPair(int leftIndex, int rightIndex) {
            this.leftIndex = leftIndex;
            this.rightIndex = rightIndex;
        }
    }

    private static final class JoinKey {
        private final List<Value> values;

        private JoinKey(List<Value> values) {
            this.values = values;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof JoinKey)) {
                return false;
            }
            return values.equals(((JoinKey) other).values);
        }

        @Override
        public int hashCode() {
            return values.hashCode();
        }
    }
}
