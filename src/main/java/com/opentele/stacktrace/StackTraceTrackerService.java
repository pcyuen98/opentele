package com.opentele.stacktrace;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class StackTraceTrackerService {

    private final Tracer tracer;
    private final List<StackTraceData> trackedStackTraces;

    @Autowired(required = false)
    private RedisStackTracePersistenceService redisService;

    public StackTraceTrackerService() {
        this.tracer = GlobalOpenTelemetry.getTracer("stacktrace-tracker");
        this.trackedStackTraces = new ArrayList<>();
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

            // Persist to Redis if service is available
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
        
        // Clear Redis if service is available
        if (redisService != null) {
            redisService.clearAllStackTraces();
        }
    }

    public int getStackTraceCount() {
        // If Redis service is available, get count from Redis
        if (redisService != null) {
            return (int) redisService.getStackTraceCount();
        }
        return trackedStackTraces.size();
    }

    public void setRedisService(RedisStackTracePersistenceService redisService) {
        this.redisService = redisService;
    }
}
