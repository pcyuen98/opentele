package com.opentele.stacktrace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opentele.stacktrace.model.StackTraceData;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class RedisStackTracePersistenceService {

    private static final String STACKTRACE_KEY_PREFIX = "stacktrace:";
    private static final String STACKTRACE_INDEX_KEY = "stacktraces:index";
    private static final String STACKTRACE_IP_INDEX_PREFIX = "stacktraces:ip:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public RedisStackTracePersistenceService(
            StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        log.debug("RedisStackTracePersistenceService initialized");
    }

    public void persistStackTrace(String traceId, StackTraceData data) {
        try {
            String key = STACKTRACE_KEY_PREFIX + traceId;
            String jsonData = objectMapper.writeValueAsString(data);

            stringRedisTemplate.opsForValue().set(key, jsonData);
            stringRedisTemplate.opsForSet().add(STACKTRACE_INDEX_KEY, traceId);
            stringRedisTemplate.opsForSet()
                    .add(STACKTRACE_IP_INDEX_PREFIX + data.getIp(), traceId);
            
            log.debug("Persisted stack trace: {}", traceId);

        } catch (Exception e) {
            log.error("Failed to serialize stack trace data for traceId: {}", traceId, e);
            throw new RuntimeException(
                    "Failed to serialize stack trace data", e);
        }
    }

    public StackTraceData retrieveStackTrace(String traceId) {
        try {
            String key = STACKTRACE_KEY_PREFIX + traceId;
            String jsonData = stringRedisTemplate.opsForValue().get(key);

            if (jsonData == null || jsonData.isBlank()) {
                return null;
            }

            return objectMapper.readValue(
                    jsonData,
                    StackTraceData.class
            );

        } catch (Exception e) {
            log.error("Failed to deserialize stack trace data for traceId: {}", traceId, e);
            throw new RuntimeException(
                    "Failed to deserialize stack trace data", e);
        }
    }

    public List<StackTraceData> retrieveAllStackTraces() {
        Set<String> traceIds =
                stringRedisTemplate.opsForSet()
                        .members(STACKTRACE_INDEX_KEY);

        List<StackTraceData> result = new ArrayList<>();

        if (traceIds != null) {
            for (String traceId : traceIds) {
                StackTraceData data = retrieveStackTrace(traceId);
                if (data != null) {
                    result.add(data);
                }
            }
        }

        log.debug("Retrieved {} stack traces from Redis", result.size());
        return result;
    }

    public List<StackTraceData> retrieveStackTracesByIp(String ip) {
        Set<String> traceIds =
                stringRedisTemplate.opsForSet()
                        .members(STACKTRACE_IP_INDEX_PREFIX + ip);

        List<StackTraceData> result = new ArrayList<>();

        if (traceIds != null) {
            for (String traceId : traceIds) {
                StackTraceData data = retrieveStackTrace(traceId);
                if (data != null) {
                    result.add(data);
                }
            }
        }

        log.debug("Retrieved {} stack traces for IP: {}", result.size(), ip);
        return result;
    }

    public void deleteStackTrace(String traceId) {
        StackTraceData data = retrieveStackTrace(traceId);

        if (data != null) {
            String key = STACKTRACE_KEY_PREFIX + traceId;

            stringRedisTemplate.delete(key);
            stringRedisTemplate.opsForSet()
                    .remove(STACKTRACE_INDEX_KEY, traceId);
            stringRedisTemplate.opsForSet()
                    .remove(STACKTRACE_IP_INDEX_PREFIX + data.getIp(), traceId);
            
            log.debug("Deleted stack trace: {}", traceId);
        }
    }

    public void clearAllStackTraces() {
        Set<String> keys =
                stringRedisTemplate.keys(STACKTRACE_KEY_PREFIX + "*");

        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }

        stringRedisTemplate.delete(STACKTRACE_INDEX_KEY);

        Set<String> ipIndexKeys =
                stringRedisTemplate.keys(STACKTRACE_IP_INDEX_PREFIX + "*");

        if (ipIndexKeys != null && !ipIndexKeys.isEmpty()) {
            stringRedisTemplate.delete(ipIndexKeys);
        }
        
        log.info("Cleared all stack traces from Redis");
    }

    public long getStackTraceCount() {
        Long count =
                stringRedisTemplate.opsForSet()
                        .size(STACKTRACE_INDEX_KEY);

        return count != null ? count : 0;
    }

    public boolean existsStackTrace(String traceId) {
        String key = STACKTRACE_KEY_PREFIX + traceId;
        return Boolean.TRUE.equals(
                stringRedisTemplate.hasKey(key)
        );
    }
}