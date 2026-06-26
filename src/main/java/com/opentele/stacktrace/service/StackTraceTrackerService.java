package com.opentele.stacktrace.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.opentele.stacktrace.exception.TelemetryException;
import com.opentele.stacktrace.model.StackTraceData;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

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
    
    public List<StackTraceData> filterStackTraces(
            LocalDateTime fromTimestamp,
            LocalDateTime toTimestamp,
            String ip,
            String errorCode,
            String messageSearch) {

        List<StackTraceData> allTraces = getAllTrackedStackTraces();

        return allTraces.stream()
                .filter(trace -> {

                    if (fromTimestamp != null
                            && trace.getTimestamp() != null
                            && trace.getTimestamp().isBefore(fromTimestamp)) {
                        return false;
                    }

                    if (toTimestamp != null
                            && trace.getTimestamp() != null
                            && trace.getTimestamp().isAfter(toTimestamp)) {
                        return false;
                    }

                    if (ip != null
                            && !ip.isBlank()
                            && !ip.equals(trace.getIp())) {
                        return false;
                    }

                    if (errorCode != null
                            && !errorCode.isBlank()
                            && !errorCode.equals(trace.getErrorCode())) {
                        return false;
                    }

                    if (messageSearch != null
                            && !messageSearch.isBlank()) {

                        if (trace.getErrorMessage() == null
                                || !trace.getErrorMessage()
                                .toLowerCase()
                                .contains(messageSearch.toLowerCase())) {
                            return false;
                        }
                    }

                    return true;
                })
                .toList();
    }

}
