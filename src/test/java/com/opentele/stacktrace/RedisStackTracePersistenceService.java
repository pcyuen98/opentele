package com.opentele.stacktrace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RedisStackTracePersistenceService {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final String KEY_PREFIX = "stacktrace:";
    private static final String ALL_KEY = "stacktrace:all";
    private static final String IP_INDEX_PREFIX = "stacktrace:ip:";

    public RedisStackTracePersistenceService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void persistStackTrace(String traceId, StackTraceData data) {
        try {
            String key = KEY_PREFIX + traceId;
            String json = mapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, json);
            redisTemplate.opsForSet().add(ALL_KEY, traceId);
            if (data.getIp() != null) {
                redisTemplate.opsForSet().add(IP_INDEX_PREFIX + data.getIp(), traceId);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public StackTraceData retrieveStackTrace(String traceId) {
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + traceId);
            if (json == null) return null;
            return mapper.readValue(json, StackTraceData.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean existsStackTrace(String traceId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + traceId));
    }

    public List<StackTraceData> retrieveAllStackTraces() {
        Set<String> ids = redisTemplate.opsForSet().members(ALL_KEY);
        if (ids == null) return new ArrayList<>();
        return ids.stream().map(this::retrieveStackTrace).collect(Collectors.toList());
    }

    public List<StackTraceData> retrieveStackTracesByIp(String ip) {
        Set<String> ids = redisTemplate.opsForSet().members(IP_INDEX_PREFIX + ip);
        if (ids == null) return new ArrayList<>();
        return ids.stream().map(this::retrieveStackTrace).collect(Collectors.toList());
    }

    public void deleteStackTrace(String traceId) {
        StackTraceData data = retrieveStackTrace(traceId);
        redisTemplate.delete(KEY_PREFIX + traceId);
        redisTemplate.opsForSet().remove(ALL_KEY, traceId);
        if (data != null && data.getIp() != null) {
            redisTemplate.opsForSet().remove(IP_INDEX_PREFIX + data.getIp(), traceId);
        }
    }

    public void clearAllStackTraces() {
        Set<String> ids = redisTemplate.opsForSet().members(ALL_KEY);
        if (ids != null) {
            ids.forEach(id -> redisTemplate.delete(KEY_PREFIX + id));
        }
        // delete any ip index sets
        Set<String> ipKeys = redisTemplate.keys(IP_INDEX_PREFIX + "*");
        if (ipKeys != null && !ipKeys.isEmpty()) {
            redisTemplate.delete(ipKeys);
        }
        redisTemplate.delete(ALL_KEY);
    }

    public long getStackTraceCount() {
        Set<String> ids = redisTemplate.opsForSet().members(ALL_KEY);
        return ids == null ? 0 : ids.size();
    }
}
