package com.opentele.stacktrace;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

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

	public String getIp() {
		return ip;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
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

	public String getErrorType() {
		return errorCode;
	}

	public void setErrorType(String errorType) {
		this.errorCode = errorType;
	}
}