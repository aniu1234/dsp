package com.qinyadan.system.dsp.engine.service.dto;

public final class MutationOutcome {

    private final MutationOperation operation;
    private final int affectedRows;

    public MutationOutcome(MutationOperation operation, int affectedRows) {
        if (operation == null || affectedRows < 0) {
            throw new IllegalArgumentException("Valid mutation operation and row count are required");
        }
        this.operation = operation;
        this.affectedRows = affectedRows;
    }

    public MutationOperation getOperation() { return operation; }
    public int getAffectedRows() { return affectedRows; }
}
