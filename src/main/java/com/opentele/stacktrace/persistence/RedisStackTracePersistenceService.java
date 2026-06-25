package com.opentele.stacktrace.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opentele.stacktrace.model.StackTraceData;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class RedisStackTracePersistenceService {

	private static final String STACKTRACE_KEY_PREFIX = "stacktrace:";
	private static final String STACKTRACE_INDEX_KEY = "stacktraces:index";
	private static final String STACKTRACE_IP_INDEX_PREFIX = "stacktraces:ip:";

	@Getter
	@Qualifier("stringRedisTemplate")
	private final RedisTemplate<String, String> stringRedisTemplate;
	
	@Getter
	private final RedisTemplate<String, StackTraceData> redisTemplate;

	@Autowired
	public RedisStackTracePersistenceService(
			@Qualifier("stringRedisTemplate") RedisTemplate<String, String> stringRedisTemplate,
			@Autowired(required = false) RedisTemplate<String, StackTraceData> redisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.redisTemplate = redisTemplate;
	}

	public void persistStackTrace(String traceId, StackTraceData data) {
		String key = STACKTRACE_KEY_PREFIX + traceId;

		if (redisTemplate != null) {
			redisTemplate.opsForValue().set(key, data);
		} else {
			persistAsJson(key, data);
		}

		stringRedisTemplate.opsForSet().add(STACKTRACE_INDEX_KEY, traceId);
		stringRedisTemplate.opsForSet().add(STACKTRACE_IP_INDEX_PREFIX + data.getIp(), traceId);
	}

	public StackTraceData retrieveStackTrace(String traceId) {
		String key = STACKTRACE_KEY_PREFIX + traceId;

		if (redisTemplate != null) {
			return redisTemplate.opsForValue().get(key);
		} else {
			return retrieveFromJson(key);
		}
	}

	public List<StackTraceData> retrieveAllStackTraces() {
		Set<String> traceIds = stringRedisTemplate.opsForSet().members(STACKTRACE_INDEX_KEY);
		List<StackTraceData> result = new ArrayList<>();

		if (traceIds != null) {
			for (String traceId : traceIds) {
				StackTraceData data = retrieveStackTrace(traceId);
				if (data != null) {
					result.add(data);
				}
			}
		}
		return result;
	}

	public List<StackTraceData> retrieveStackTracesByIp(String ip) {
		Set<String> traceIds = stringRedisTemplate.opsForSet().members(STACKTRACE_IP_INDEX_PREFIX + ip);
		List<StackTraceData> result = new ArrayList<>();

		if (traceIds != null) {
			for (String traceId : traceIds) {
				StackTraceData data = retrieveStackTrace(traceId);
				if (data != null) {
					result.add(data);
				}
			}
		}
		return result;
	}

	public void deleteStackTrace(String traceId) {
		StackTraceData data = retrieveStackTrace(traceId);
		if (data != null) {
			String key = STACKTRACE_KEY_PREFIX + traceId;
			stringRedisTemplate.delete(key);
			stringRedisTemplate.opsForSet().remove(STACKTRACE_INDEX_KEY, traceId);
			stringRedisTemplate.opsForSet().remove(STACKTRACE_IP_INDEX_PREFIX + data.getIp(), traceId);
		}
	}

	public void clearAllStackTraces() {
		Set<String> keys = stringRedisTemplate.keys(STACKTRACE_KEY_PREFIX + "*");
		if (keys != null && !keys.isEmpty()) {
			stringRedisTemplate.delete(keys);
		}
		stringRedisTemplate.delete(STACKTRACE_INDEX_KEY);
		Set<String> ipIndexKeys = stringRedisTemplate.keys(STACKTRACE_IP_INDEX_PREFIX + "*");
		if (ipIndexKeys != null && !ipIndexKeys.isEmpty()) {
			stringRedisTemplate.delete(ipIndexKeys);
		}
	}

	public long getStackTraceCount() {
		Long count = stringRedisTemplate.opsForSet().size(STACKTRACE_INDEX_KEY);
		return count != null ? count : 0;
	}

	public boolean existsStackTrace(String traceId) {
		String key = STACKTRACE_KEY_PREFIX + traceId;
		return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
	}

	private void persistAsJson(String key, StackTraceData data) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(new JavaTimeModule());
			String jsonData = mapper.writeValueAsString(data);
			stringRedisTemplate.opsForValue().set(key, jsonData);
		} catch (Exception e) {
			throw new RuntimeException("Failed to serialize stack trace data", e);
		}
	}

	private StackTraceData retrieveFromJson(String key) {
		try {
			String jsonData = stringRedisTemplate.opsForValue().get(key);
			if (jsonData == null) {
				return null;
			}

			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(new JavaTimeModule());
			return mapper.readValue(jsonData, StackTraceData.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to deserialize stack trace data", e);
		}
	}
}
