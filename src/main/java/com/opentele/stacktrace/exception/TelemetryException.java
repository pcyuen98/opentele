package com.opentele.stacktrace.exception;

import lombok.Getter;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.UUID;

@Getter
public class TelemetryException extends RuntimeException implements Serializable {

	private static final long serialVersionUID = 1L;

	private final UUID id;
	private final String errorCode;
	private final String stackTraceString;

	public TelemetryException(String message) {
		super(message);
		this.id = UUID.randomUUID();
		this.errorCode = this.getClass().getName();
		this.stackTraceString = buildStackTrace(this);
	}

	public TelemetryException(String message, Throwable cause) {
		super(message, cause);
		this.id = UUID.randomUUID();
		this.errorCode = (cause != null)
				? cause.getClass().getName()
				: this.getClass().getName();
		this.stackTraceString = buildStackTrace(
				cause != null ? cause : this
		);
	}

	public TelemetryException(String message,
							  String errorType,
							  String stackTraceString) {
		super(message);
		this.id = UUID.randomUUID();
		this.errorCode = errorType;
		this.stackTraceString = stackTraceString;
	}

	public String getErrorType() {
		return errorCode;
	}

	private static String buildStackTrace(Throwable throwable) {
		if (throwable == null) {
			return "";
		}

		StringWriter sw = new StringWriter();
		try (PrintWriter pw = new PrintWriter(sw)) {
			throwable.printStackTrace(pw);
			return sw.toString();
		}
	}

	@Override
	public String toString() {
		return "TelemetryException{" +
				"id=" + id +
				", message='" + getMessage() + '\'' +
				", errorType='" + errorCode + '\'' +
				", stackTraceString='" + stackTraceString + '\'' +
				'}';
	}
}
