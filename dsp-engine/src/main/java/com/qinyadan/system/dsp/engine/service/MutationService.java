package com.qinyadan.system.dsp.engine.service;

import com.qinyadan.system.dsp.core.config.DspConfiguration;
import com.qinyadan.system.dsp.core.data.type.DataType;
import com.qinyadan.system.dsp.core.data.value.Value;
import com.qinyadan.system.dsp.engine.calcite.SlothTable;
import com.qinyadan.system.dsp.engine.calcite.SlothTableEngine;
import com.qinyadan.system.dsp.engine.data.SlothRow;
import com.qinyadan.system.dsp.engine.operator.Operator;
import com.qinyadan.system.dsp.engine.operator.QueryResourceLimitException;
import com.qinyadan.system.dsp.engine.service.dto.MutationAssignment;
import com.qinyadan.system.dsp.engine.service.dto.MutationOperation;
import com.qinyadan.system.dsp.engine.service.dto.MutationOutcome;
import com.qinyadan.system.dsp.engine.service.dto.MutationRequest;
import com.qinyadan.system.dsp.engine.service.dto.QueryExecution;
import com.qinyadan.system.dsp.engine.service.dto.WriteColumn;
import com.qinyadan.system.dsp.engine.service.dto.WriteTable;
import org.apache.calcite.sql.SqlNode;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Application boundary for single-table, single-shard autocommit mutations.
 */
public final class MutationService {

    public static final MutationService INSTANCE = new MutationService();

    private MutationService() {
    }

    public MutationOutcome mutate(MutationRequest request) {
        try {
            return doMutate(request);
        } catch (WriteServiceException e) {
            RuntimeMetrics.INSTANCE.mutationFailed();
            throw e;
        } catch (UnsupportedOperationException e) {
            RuntimeMetrics.INSTANCE.mutationFailed();
            throw new WriteServiceException(WriteErrorCode.UNSUPPORTED_OPERATION,
                    e.getMessage(), e);
        } catch (RuntimeException e) {
            RuntimeMetrics.INSTANCE.mutationFailed();
            throw new WriteServiceException(WriteErrorCode.STORAGE_FAILURE,
                    "Storage mutation failed", e);
        }
    }

    private MutationOutcome doMutate(MutationRequest request) {
        if (request == null) {
            throw failure(WriteErrorCode.INVALID_REQUEST, "Mutation request is required");
        }
        WriteTable table = WriteService.INSTANCE.describe(
                request.getDatabase(), request.getTable());
        SlothTable slothTable = CatalogService.INSTANCE.getTable(
                request.getDatabase(), request.getTable());
        if (table == null || slothTable == null || slothTable.getSlothTableEngine() == null) {
            throw failure(WriteErrorCode.UNKNOWN_TABLE,
                    "Unknown table: " + request.getDatabase() + "." + request.getTable());
        }
        SlothTableEngine engine = slothTable.getSlothTableEngine();
        if (engine.getStorageEngines() == null || engine.getStorageEngines().size() != 1) {
            throw failure(WriteErrorCode.UNSUPPORTED_OPERATION,
                    "UPDATE and DELETE currently require a single-shard table");
        }

        synchronized (engine) {
            return mutateLocked(request, table, engine);
        }
    }

    private MutationOutcome mutateLocked(MutationRequest request, WriteTable table,
                                         SlothTableEngine engine) {
        List<AssignmentPlan> assignments = resolveAssignments(request, table);
        List<List<Value>> selectedRows = executeProjection(request, table, assignments);
        if (selectedRows.isEmpty()) {
            RuntimeMetrics.INSTANCE.mutationSucceeded(0);
            return new MutationOutcome(request.getOperation(), 0);
        }

        int columnCount = table.getColumns().size();
        Map<List<Value>, Deque<List<Value>>> changes = new HashMap<>();
        List<List<Value>> replacements = new ArrayList<>(selectedRows.size());
        int affectedRows = 0;
        for (List<Value> selected : selectedRows) {
            if (selected.size() != columnCount + assignments.size()) {
                throw failure(WriteErrorCode.STORAGE_FAILURE,
                        "Mutation projection returned an unexpected column count");
            }
            List<Value> original = new ArrayList<>(selected.subList(0, columnCount));
            List<Value> replacement = null;
            if (request.getOperation() == MutationOperation.UPDATE) {
                replacement = new ArrayList<>(original);
                for (int i = 0; i < assignments.size(); i++) {
                    AssignmentPlan assignment = assignments.get(i);
                    Value evaluated = selected.get(columnCount + i);
                    replacement.set(assignment.columnIndex,
                            assignment.value(evaluated));
                }
                if (replacement.equals(original)) {
                    continue;
                }
                replacements.add(replacement);
            }
            changes.computeIfAbsent(original,
                    ignored -> new LinkedList<List<Value>>()).add(replacement);
            affectedRows++;
        }
        if (affectedRows == 0) {
            RuntimeMetrics.INSTANCE.mutationSucceeded(0);
            return new MutationOutcome(request.getOperation(), 0);
        }
        WriteService.INSTANCE.validateMutationRows(table, replacements);

        List<List<Value>> snapshot = engine.readAllRowsForMutation();
        List<List<Value>> mutatedRows = new ArrayList<>(snapshot.size());
        for (List<Value> row : snapshot) {
            Deque<List<Value>> replacementsForRow = changes.get(row);
            if (replacementsForRow == null || replacementsForRow.isEmpty()) {
                mutatedRows.add(row);
                continue;
            }
            List<Value> replacement = replacementsForRow.removeFirst();
            if (replacement != null) {
                mutatedRows.add(replacement);
            }
        }
        for (Deque<List<Value>> remaining : changes.values()) {
            if (!remaining.isEmpty()) {
                throw failure(WriteErrorCode.STORAGE_FAILURE,
                        "Table changed while the mutation was being prepared");
            }
        }

        try {
            engine.replaceAllRows(mutatedRows);
        } catch (UnsupportedOperationException e) {
            throw new WriteServiceException(WriteErrorCode.UNSUPPORTED_OPERATION,
                    e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new WriteServiceException(WriteErrorCode.STORAGE_FAILURE,
                    "Storage mutation failed", e);
        }
        RuntimeMetrics.INSTANCE.mutationSucceeded(affectedRows);
        return new MutationOutcome(request.getOperation(), affectedRows);
    }

    private List<AssignmentPlan> resolveAssignments(MutationRequest request,
                                                     WriteTable table) {
        if (request.getOperation() == MutationOperation.DELETE) {
            return new ArrayList<>();
        }
        List<AssignmentPlan> result = new ArrayList<>(request.getAssignments().size());
        Set<String> seen = new HashSet<>();
        for (MutationAssignment assignment : request.getAssignments()) {
            WriteColumn column = table.getColumn(assignment.getColumn());
            if (column == null) {
                throw failure(WriteErrorCode.UNKNOWN_COLUMN,
                        "Unknown mutation column: " + assignment.getColumn());
            }
            String key = column.getName().toLowerCase(Locale.ROOT);
            if (!seen.add(key)) {
                throw failure(WriteErrorCode.DUPLICATE_COLUMN,
                        "Mutation column was specified twice: " + assignment.getColumn());
            }
            result.add(new AssignmentPlan(table.getColumnNames().indexOf(column.getName()),
                    column, assignment));
        }
        return result;
    }

    private List<List<Value>> executeProjection(MutationRequest request, WriteTable table,
                                                List<AssignmentPlan> assignments) {
        String sql = projectionSql(request, table, assignments);
        final QueryExecution execution;
        try {
            SqlNode sqlNode = QueryService.INSTANCE.parse(sql, request.getDatabase());
            execution = QueryService.INSTANCE.prepare(sql, request.getDatabase(), sqlNode);
        } catch (Exception e) {
            throw new WriteServiceException(WriteErrorCode.INVALID_REQUEST,
                    "Invalid mutation expression or condition", e);
        }

        Operator<SlothRow> operator = execution.getOperator();
        List<List<Value>> rows = new ArrayList<>();
        RuntimeException failure = null;
        boolean openAttempted = false;
        DspConfiguration configuration = DspConfiguration.load();
        long timeoutMillis = configuration.getQueryTimeoutMillis();
        long timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        long startedAt = System.nanoTime();
        int maximumRows = configuration.getWriteMaxRowsPerMutation();
        try {
            openAttempted = true;
            operator.open();
            while (true) {
                checkDeadline(startedAt, timeoutNanos, timeoutMillis);
                SlothRow row = operator.next();
                if (row == SlothRow.EOF_ROW) {
                    break;
                }
                if (rows.size() >= maximumRows) {
                    throw new QueryResourceLimitException(
                            "mutation exceeded the row limit of " + maximumRows);
                }
                rows.add(new ArrayList<>(row.getAllColumn()));
            }
        } catch (RuntimeException e) {
            failure = e;
        } finally {
            if (openAttempted) {
                try {
                    operator.close();
                } catch (RuntimeException closeError) {
                    if (failure == null) {
                        failure = closeError;
                    } else {
                        failure.addSuppressed(closeError);
                    }
                }
            }
        }
        if (failure instanceof QueryResourceLimitException) {
            throw failure(WriteErrorCode.RESOURCE_LIMIT, failure.getMessage());
        }
        if (failure instanceof WriteServiceException) {
            throw (WriteServiceException) failure;
        }
        if (failure != null) {
            throw new WriteServiceException(WriteErrorCode.INVALID_REQUEST,
                    "Mutation expression or condition failed", failure);
        }
        return rows;
    }

    private String projectionSql(MutationRequest request, WriteTable table,
                                 List<AssignmentPlan> assignments) {
        StringBuilder sql = new StringBuilder("SELECT ");
        String qualifier = request.getAlias() == null ? ""
                : quote(request.getAlias()) + ".";
        for (int i = 0; i < table.getColumns().size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(qualifier).append(quote(table.getColumns().get(i).getName()));
        }
        for (AssignmentPlan assignment : assignments) {
            sql.append(", ");
            if (assignment.assignment.isDefaultValue()
                    || assignment.assignment.isNullValue()) {
                sql.append(qualifier).append(quote(assignment.column.getName()));
            } else {
                sql.append(assignment.assignment.getExpressionSql());
            }
        }
        sql.append(" FROM ").append(quote(request.getDatabase())).append(".")
                .append(quote(request.getTable()));
        if (request.getAlias() != null) {
            sql.append(" AS ").append(quote(request.getAlias()));
        }
        if (request.getConditionSql() != null) {
            sql.append(" WHERE ").append(request.getConditionSql());
        }
        return sql.toString();
    }

    private void checkDeadline(long startedAt, long timeoutNanos, long timeoutMillis) {
        if (timeoutMillis > 0 && System.nanoTime() - startedAt > timeoutNanos) {
            throw failure(WriteErrorCode.RESOURCE_LIMIT,
                    "Mutation exceeded the query timeout of " + timeoutMillis + " ms");
        }
    }

    private String quote(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private WriteServiceException failure(WriteErrorCode code, String message) {
        return new WriteServiceException(code, message);
    }

    private static final class AssignmentPlan {
        private final int columnIndex;
        private final WriteColumn column;
        private final MutationAssignment assignment;

        private AssignmentPlan(int columnIndex, WriteColumn column,
                               MutationAssignment assignment) {
            this.columnIndex = columnIndex;
            this.column = column;
            this.assignment = assignment;
        }

        private Value value(Value evaluated) {
            DataType<?> targetType = column.getDataType();
            try {
                if (assignment.isDefaultValue()) {
                    return targetType.createByType(column.getDefaultValue());
                }
                if (assignment.isNullValue()) {
                    return targetType.createByType(null);
                }
                Object raw = evaluated == null ? null : evaluated.getValueByType();
                return targetType.createByType(raw);
            } catch (RuntimeException e) {
                throw new WriteServiceException(WriteErrorCode.TYPE_MISMATCH,
                        "Cannot assign expression to column '" + column.getName() + "'", e);
            }
        }
    }
}
