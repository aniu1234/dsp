package com.qinyadan.system.dsp.schema.common.spi;

import java.util.Objects;


public final class ConnectorHealth {

    public enum Status {
        READY,
        UNAVAILABLE
    }

    private final Status status;
    private final String message;

    private ConnectorHealth(Status status, String message) {
        this.status = Objects.requireNonNull(status, "status");
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Connector health message must not be empty");
        }
        this.message = message.trim();
    }

    public static ConnectorHealth ready(String message) {
        return new ConnectorHealth(Status.READY, message);
    }

    public static ConnectorHealth unavailable(String message) {
        return new ConnectorHealth(Status.UNAVAILABLE, message);
    }

    public static ConnectorHealth unavailable(String message, Throwable cause) {
        if (cause == null) {
            return unavailable(message);
        }
        String detail = cause.getMessage();
        String suffix = cause.getClass().getSimpleName()
                + (detail == null || detail.trim().isEmpty() ? "" : ": " + detail.trim());
        return unavailable(message + " (" + suffix + ")");
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public boolean isReady() {
        return status == Status.READY;
    }

    @Override
    public String toString() {
        return status + ": " + message;
    }
}
