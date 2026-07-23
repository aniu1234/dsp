package com.qinyadan.system.dsp.engine.operator;

import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.type.DataTypes;
import com.qinyadan.system.dsp.core.data.value.Value;
import com.qinyadan.system.dsp.engine.data.SlothRow;
import org.apache.calcite.rel.core.AggregateCall;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.sql.SqlAggFunction;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.util.ImmutableBitSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Incremental hash aggregation. Non-distinct aggregates retain one state per
 * group instead of retaining every input row. DISTINCT aggregates retain only
 * their unique scalar inputs and remain protected by the materialization limit.
 */
public class SlothAggregateOperator extends AbstractOperator<SlothRow> {

    private final Operator<SlothRow> input;
    private final ImmutableBitSet groupset;
    private final List<ImmutableBitSet> groupSets;
    private final List<AggregateCall> aggregateCalls;

    private List<Integer> groupByIndex;
    private final Map<List<Value>, GroupState> groups = new LinkedHashMap<>();
    private Iterator<GroupState> groupIterator;
    private boolean aggregated;
    private long materializedUnits;

    public SlothAggregateOperator(Operator<SlothRow> input, ImmutableBitSet groupset,
                                  List<ImmutableBitSet> groupSets,
                                  List<AggregateCall> aggregateCalls,
                                  RelDataType rowType) {
        super(rowType);
        this.input = input;
        this.groupset = groupset;
        this.groupSets = groupSets;
        this.aggregateCalls = aggregateCalls;
    }

    @Override
    public void open() {
        groups.clear();
        groupIterator = null;
        aggregated = false;
        materializedUnits = 0;
        input.open();
        groupByIndex = groupset.asList();
        validateGroupingSets();
        validateAggregateCalls();
    }

    @Override
    public SlothRow next() {
        if (!aggregated) {
            aggregateInput();
        }
        if (!groupIterator.hasNext()) {
            return SlothRow.EOF_ROW;
        }
        GroupState group = groupIterator.next();
        List<Value> result = new ArrayList<>(
                group.key.size() + group.accumulators.size());
        for (Value value : group.key) {
            result.add(value.copy());
        }
        for (Accumulator accumulator : group.accumulators) {
            result.add(accumulator.result());
        }
        return new SlothRow(result);
    }

    private void aggregateInput() {
        // SQL global aggregation over an empty relation still returns one row.
        if (groupByIndex.isEmpty()) {
            addGroup(Collections.<Value>emptyList());
        }

        SlothRow row;
        while ((row = input.next()) != SlothRow.EOF_ROW) {
            List<Value> key = groupKey(row);
            GroupState group = groups.get(key);
            if (group == null) {
                group = addGroup(key);
            }
            for (Accumulator accumulator : group.accumulators) {
                if (accumulator.add(row)) {
                    materializedUnits++;
                    checkMaterializedUnits();
                }
            }
        }
        groupIterator = groups.values().iterator();
        aggregated = true;
    }

    private GroupState addGroup(List<Value> key) {
        GroupState group = new GroupState(key, createAccumulators());
        groups.put(key, group);
        materializedUnits++;
        checkMaterializedUnits();
        return group;
    }

    private List<Value> groupKey(SlothRow row) {
        if (groupByIndex.isEmpty()) {
            return Collections.emptyList();
        }
        List<Value> key = new ArrayList<>(groupByIndex.size());
        for (Integer index : groupByIndex) {
            key.add(row.getColumn(index).copy());
        }
        return key;
    }

    private List<Accumulator> createAccumulators() {
        List<Accumulator> result = new ArrayList<>(aggregateCalls.size());
        for (AggregateCall call : aggregateCalls) {
            List<Integer> arguments = call.getArgList();
            if (arguments.size() > 1) {
                throw new UnsupportedOperationException(
                        "Multi-argument aggregate is not supported: " + call);
            }
            int index = arguments.isEmpty() ? -1 : arguments.get(0);
            DataType inputType = index < 0 ? null : input.getRowType().get(index);
            DataType resultType = com.qinyadan.system.dsp.core.util.TypeConversionUtils
                    .getBySqlTypeName(call.type.getSqlTypeName());
            SqlAggFunction function = call.getAggregation();
            if (function == SqlStdOperatorTable.COUNT) {
                if (call.isDistinct() && arguments.isEmpty()) {
                    throw new UnsupportedOperationException("COUNT(DISTINCT *) is not supported");
                }
                result.add(new CountAccumulator(index, call.isDistinct(),
                        resultType, arguments.isEmpty()));
            } else if (function == SqlStdOperatorTable.SUM
                    || function == SqlStdOperatorTable.SUM0) {
                result.add(new SumAccumulator(index, call.isDistinct(), inputType,
                        resultType, function == SqlStdOperatorTable.SUM0));
            } else if (function == SqlStdOperatorTable.MIN) {
                // DISTINCT cannot change MIN/MAX, so retaining every unique value
                // would only waste memory.
                result.add(new ExtremumAccumulator(index, false, resultType, false));
            } else if (function == SqlStdOperatorTable.MAX) {
                result.add(new ExtremumAccumulator(index, false, resultType, true));
            } else {
                throw new UnsupportedOperationException(
                        "Unsupported aggregate function: " + function.getName());
            }
        }
        return result;
    }

    private void validateAggregateCalls() {
        // Construct once so unsupported functions fail even for an empty GROUP BY input.
        createAccumulators();
    }

    private void validateGroupingSets() {
        if (groupSets == null || groupSets.size() != 1
                || !groupset.equals(groupSets.get(0))) {
            throw new UnsupportedOperationException("GROUPING SETS is not supported");
        }
    }

    private void checkMaterializedUnits() {
        QueryResourceLimits.checkMaterializedRows(materializedUnits, "aggregate state");
    }

    @Override
    public void close() {
        input.close();
        groups.clear();
        groupIterator = null;
    }

    private interface Accumulator {
        /**
         * @return true when a DISTINCT value was newly retained in memory
         */
        boolean add(SlothRow row);

        Value result();
    }

    private abstract static class ScalarAccumulator implements Accumulator {
        protected final int index;
        protected final DataType resultType;
        private final Set<Value> distinctValues;

        private ScalarAccumulator(int index, boolean distinct, DataType resultType) {
            this.index = index;
            this.resultType = resultType;
            this.distinctValues = distinct ? new HashSet<Value>() : null;
        }

        protected AddDecision accept(Value value) {
            if (value.isNull()) {
                return AddDecision.SKIP;
            }
            if (distinctValues == null) {
                return AddDecision.ACCEPT;
            }
            return distinctValues.add(value.copy())
                    ? AddDecision.ACCEPT_AND_RETAIN : AddDecision.SKIP;
        }
    }

    private static final class CountAccumulator extends ScalarAccumulator {
        private final boolean countAll;
        private long count;

        private CountAccumulator(int index, boolean distinct,
                                 DataType resultType, boolean countAll) {
            super(index, distinct, resultType);
            this.countAll = countAll;
        }

        @Override
        public boolean add(SlothRow row) {
            if (countAll) {
                count++;
                return false;
            }
            AddDecision decision = accept(row.getColumn(index));
            if (decision != AddDecision.SKIP) {
                count++;
            }
            return decision == AddDecision.ACCEPT_AND_RETAIN;
        }

        @Override
        public Value result() {
            return new Value(count, resultType);
        }
    }

    private static final class SumAccumulator extends ScalarAccumulator {
        private final boolean decimal;
        private long longSum;
        private double doubleSum;
        private boolean hasValue;
        private final boolean zeroOnEmpty;

        private SumAccumulator(int index, boolean distinct,
                               DataType inputType, DataType resultType,
                               boolean zeroOnEmpty) {
            super(index, distinct, resultType);
            this.decimal = DataTypes.DECIMAL_TYPES.contains(inputType);
            this.zeroOnEmpty = zeroOnEmpty;
        }

        @Override
        public boolean add(SlothRow row) {
            Value value = row.getColumn(index);
            AddDecision decision = accept(value);
            if (decision == AddDecision.SKIP) {
                return false;
            }
            if (decimal) {
                doubleSum += value.doubleValue();
            } else {
                longSum += value.longValue();
            }
            hasValue = true;
            return decision == AddDecision.ACCEPT_AND_RETAIN;
        }

        @Override
        public Value result() {
            if (!hasValue) {
                if (!zeroOnEmpty) {
                    return Value.nullValue(resultType);
                }
                return decimal ? new Value(0.0d, resultType)
                        : new Value(0L, resultType);
            }
            if (decimal) {
                return new Value(doubleSum, resultType);
            }
            return new Value(longSum, resultType);
        }
    }

    private static final class ExtremumAccumulator extends ScalarAccumulator {
        private final boolean maximum;
        private Value selected;

        private ExtremumAccumulator(int index, boolean distinct,
                                    DataType resultType, boolean maximum) {
            super(index, distinct, resultType);
            this.maximum = maximum;
        }

        @Override
        public boolean add(SlothRow row) {
            Value value = row.getColumn(index);
            AddDecision decision = accept(value);
            if (decision == AddDecision.SKIP) {
                return false;
            }
            if (selected == null || (maximum
                    ? value.compareTo(selected) > 0 : value.compareTo(selected) < 0)) {
                selected = value.copy();
            }
            return decision == AddDecision.ACCEPT_AND_RETAIN;
        }

        @Override
        public Value result() {
            return selected == null ? Value.nullValue(resultType)
                    : new Value(selected.getValue(), resultType);
        }
    }

    private enum AddDecision {
        SKIP,
        ACCEPT,
        ACCEPT_AND_RETAIN
    }

    private static final class GroupState {
        private final List<Value> key;
        private final List<Accumulator> accumulators;

        private GroupState(List<Value> key, List<Accumulator> accumulators) {
            this.key = key;
            this.accumulators = accumulators;
        }
    }
}
