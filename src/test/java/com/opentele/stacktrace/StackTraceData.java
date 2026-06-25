package com.opentele.stacktrace;

import java.time.LocalDateTime;
import java.util.UUID;

public class StackTraceData {
    private UUID id;
    private String ip;
    private LocalDateTime timestamp;
    private String date;
    private String stackTrace;
    private String errorMessage;
    private String errorCode;

    public StackTraceData() {}

    public StackTraceData(String ip, TelemetryException ex) {
        this.id = UUID.randomUUID();
        this.ip = ip;
        this.timestamp = LocalDateTime.now();
        this.date = this.timestamp.toLocalDate().toString();
        this.stackTrace = ex.getStackTraceString();
        this.errorMessage = ex.getMessage();
        this.errorCode = ex.getErrorType();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
