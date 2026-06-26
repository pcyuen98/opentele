package com.opentele.stacktrace;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.opentele.stacktrace.exception.TelemetryException;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StackTraceData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String ip;
    private LocalDateTime timestamp;
    private String date;
    private String stackTrace;
    private String errorMessage;
    private String errorCode;
    private UUID id;

    public StackTraceData(String ip, TelemetryException exception) {
        this.ip = ip;
        this.timestamp = LocalDateTime.now();
        this.date = this.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE);
        this.stackTrace = exception.getStackTraceString();
        this.errorMessage = exception.getMessage();
        this.errorCode = exception.getErrorType();
        this.id = exception.getId();
    }

}