package com.opentele.stacktrace;

public class TelemetryException extends Exception {
    private final String errorType;
    private final String stackTraceString;

    public TelemetryException(String message, String errorType, String stackTraceString) {
        super(message);
        this.errorType = errorType;
        this.stackTraceString = stackTraceString;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getStackTraceString() {
        return stackTraceString;
    }
}
