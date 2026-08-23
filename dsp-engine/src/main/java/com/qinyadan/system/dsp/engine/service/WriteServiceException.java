package com.qinyadan.system.dsp.engine.service;

public class WriteServiceException extends RuntimeException {

    private final WriteErrorCode errorCode;

    public WriteServiceException(WriteErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public WriteServiceException(WriteErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public WriteErrorCode getErrorCode() {
        return errorCode;
    }
}
