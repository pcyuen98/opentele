package com.opentele.stacktrace.service;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import com.opentele.stacktrace.exception.TelemetryException;
import com.opentele.stacktrace.model.StackTraceData;
import com.opentele.stacktrace.persistence.RedisStackTracePersistenceService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StackTraceTrackerService {

	private final Tracer tracer;
	private final List<StackTraceData> trackedStackTraces;
	
	@Setter
	private RedisStackTracePersistenceService redisService;

	public StackTraceTrackerService() {
		this.tracer = GlobalOpenTelemetry.getTracer("stacktrace-tracker");
		this.trackedStackTraces = new ArrayList<>();
		this.redisService = null;
	}

	public StackTraceData trackStackTrace(String ip, TelemetryException exception) {
		Span span = tracer.spanBuilder("track-stacktrace")
				.setAttribute("ip", ip)
				.setAttribute("error.type", exception.getClass().getName())
				.setAttribute("error.message", exception.getMessage())
				.startSpan();

		try {
			StackTraceData data = new StackTraceData(ip, exception);
			String traceId = UUID.randomUUID().toString();

			trackedStackTraces.add(data);

			if (redisService != null) {
				redisService.persistStackTrace(traceId, data);
			}

			span.addEvent("stacktrace_tracked", io.opentelemetry.api.common.Attributes.builder()
					.put("ip", ip)
					.put("trace_id", traceId)
					.put("error_count", trackedStackTraces.size())
					.build());

			return data;
		} finally {
			span.end();
		}
	}

	public List<StackTraceData> getAllTrackedStackTraces() {
		if (redisService != null) {
			return redisService.retrieveAllStackTraces();
		}
		return new ArrayList<>(trackedStackTraces);
	}

	public List<StackTraceData> getStackTracesByIp(String ip) {
		if (redisService != null) {
			return redisService.retrieveStackTracesByIp(ip);
		}
		return trackedStackTraces.stream()
				.filter(st -> st.getIp().equals(ip))
				.toList();
	}

	public List<StackTraceData> filterStackTraces(LocalDate startDate, LocalDate endDate, String ip, String errorCode, String messageSearch) {
		List<StackTraceData> allTraces = getAllTrackedStackTraces();
		
		return allTraces.stream()
				.filter(trace -> {
					if (startDate != null && trace.getDate() != null) {
						LocalDate traceDate = LocalDate.parse(trace.getDate());
						if (traceDate.isBefore(startDate)) {
							return false;
						}
					}
					
					if (endDate != null && trace.getDate() != null) {
						LocalDate traceDate = LocalDate.parse(trace.getDate());
						if (traceDate.isAfter(endDate)) {
							return false;
						}
					}
					
					if (ip != null && !ip.isEmpty() && !trace.getIp().equals(ip)) {
						return false;
					}
					
					if (errorCode != null && !errorCode.isEmpty() && !trace.getErrorCode().equals(errorCode)) {
						return false;
					}
					
					if (messageSearch != null && !messageSearch.isEmpty()) {
						if (trace.getErrorMessage() == null || !trace.getErrorMessage().toLowerCase().contains(messageSearch.toLowerCase())) {
							return false;
						}
					}
					
					return true;
				})
				.toList();
	}

	public void clearTrackedStackTraces() {
		trackedStackTraces.clear();

		if (redisService != null) {
			redisService.clearAllStackTraces();
		}
	}

	public int getStackTraceCount() {
		if (redisService != null) {
			return (int) redisService.getStackTraceCount();
		}
		return trackedStackTraces.size();
	}
}

