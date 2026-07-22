package com.qinyadan.system.dsp.engine.service;

/**
 * Structured insert failure that protocol adapters can map to wire errors.
 */
public final class InsertException extends RuntimeException {

    public enum Reason {
        UNSUPPORTED_SOURCE,
        UNSUPPORTED_EXPRESSION,
        SCHEMA_NOT_FOUND,
        TABLE_NOT_FOUND,
        UNKNOWN_COLUMN,
        DUPLICATE_COLUMN,
        COLUMN_COUNT_MISMATCH,
        COLUMN_CANNOT_BE_NULL,
        INVALID_VALUE
    }

    private final Reason reason;
    private final String name;
    private final String value;
    private final int row;

    InsertException(Reason reason, String name, String value, int row, Throwable cause) {
        super(reason + (name == null ? "" : ": " + name), cause);
        this.reason = reason;
        this.name = name;
        this.value = value;
        this.row = row;
    }

    public Reason getReason() {
        return reason;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public int getRow() {
        return row;
    }
}
