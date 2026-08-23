package com.qinyadan.system.dsp.engine.service;

public enum WriteErrorCode {
    UNKNOWN_TABLE,
    RESOURCE_LIMIT,
    INVALID_REQUEST,
    TYPE_MISMATCH,
    CONSTRAINT_VIOLATION,
    IDEMPOTENCY_CONFLICT,
    STORAGE_FAILURE
}
