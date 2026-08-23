package com.qinyadan.system.dsp.engine.service.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable, parser-neutral request for an autocommit table mutation.
 */
public final class MutationRequest {

    private final MutationOperation operation;
    private final String database;
    private final String table;
    private final String alias;
    private final String conditionSql;
    private final List<MutationAssignment> assignments;

    public MutationRequest(MutationOperation operation, String database, String table,
                           String alias, String conditionSql,
                           List<MutationAssignment> assignments) {
        if (operation == null || blank(database) || blank(table)) {
            throw new IllegalArgumentException(
                    "Mutation operation, database and table are required");
        }
        List<MutationAssignment> copied = assignments == null
                ? Collections.<MutationAssignment>emptyList() : new ArrayList<>(assignments);
        if (operation == MutationOperation.UPDATE && copied.isEmpty()) {
            throw new IllegalArgumentException("UPDATE assignments are required");
        }
        if (operation == MutationOperation.DELETE && !copied.isEmpty()) {
            throw new IllegalArgumentException("DELETE cannot contain assignments");
        }
        this.operation = operation;
        this.database = database;
        this.table = table;
        this.alias = blank(alias) ? null : alias;
        this.conditionSql = blank(conditionSql) ? null : conditionSql;
        this.assignments = Collections.unmodifiableList(copied);
    }

    public MutationOperation getOperation() { return operation; }
    public String getDatabase() { return database; }
    public String getTable() { return table; }
    public String getAlias() { return alias; }
    public String getConditionSql() { return conditionSql; }
    public List<MutationAssignment> getAssignments() { return assignments; }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
