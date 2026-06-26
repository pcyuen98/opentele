package com.opentele.stacktrace;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class StackTraceTrackerService {

    private final Tracer tracer;
    private final List<StackTraceData> trackedStackTraces;

    @Autowired(required = false)
    @Setter
    private RedisStackTracePersistenceService redisService;

    public StackTraceTrackerService() {
        this.tracer = GlobalOpenTelemetry.getTracer("stacktrace-tracker");
        this.trackedStackTraces = new ArrayList<>();
        log.debug("StackTraceTrackerService initialized");
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
            log.info("Tracked stack trace with ID: {} for IP: {}", traceId, ip);

            // Persist to Redis if service is available
            if (redisService != null) {
                redisService.persistStackTrace(traceId, data);
                log.debug("Persisted stack trace to Redis");
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
        // If Redis service is available, retrieve from Redis
        if (redisService != null) {
            return redisService.retrieveAllStackTraces();
        }
        return new ArrayList<>(trackedStackTraces);
    }

    public List<StackTraceData> getStackTracesByIp(String ip) {
        // If Redis service is available, retrieve from Redis
        if (redisService != null) {
            return redisService.retrieveStackTracesByIp(ip);
        }
        return trackedStackTraces.stream()
                .filter(st -> st.getIp().equals(ip))
                .toList();
    }

    public void clearTrackedStackTraces() {
        trackedStackTraces.clear();
        log.info("Cleared all tracked stack traces");
        
        // Clear Redis if service is available
        if (redisService != null) {
            redisService.clearAllStackTraces();
            log.debug("Cleared stack traces from Redis");
        }
    }

    public int getStackTraceCount() {
        // If Redis service is available, get count from Redis
        if (redisService != null) {
            return (int) redisService.getStackTraceCount();
        }
        return trackedStackTraces.size();
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

}
