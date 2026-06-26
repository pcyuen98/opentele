package com.opentele.stacktrace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.opentele.stacktrace.exception.TelemetryException;

import redis.embedded.RedisServer;

@SpringBootTest
@ActiveProfiles("test")
@Import(RedisStackTracePersistenceServiceTest.EmbeddedRedisConfiguration.class)
class RedisStackTracePersistenceServiceTest {

    @TestConfiguration
    static class EmbeddedRedisConfiguration {

        private RedisServer redisServer;

        @PostConstruct
        void startRedis() throws IOException {
            redisServer = new RedisServer(6379);
            redisServer.start();
        }

        @PreDestroy
        void stopRedis() throws IOException {
            if (redisServer != null) {
                redisServer.stop();
            }
        }
    }

    @Autowired
    private RedisStackTracePersistenceService persistenceService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private StackTraceData testData;
    private String testTraceId;

    @BeforeEach
    void setUp() {
        testTraceId = "trace-" + UUID.randomUUID();

        testData = new StackTraceData();
        testData.setId(UUID.randomUUID());
        testData.setIp("192.168.1.1");
        testData.setTimestamp(LocalDateTime.now());
        testData.setDate("2024-06-25");
        testData.setStackTrace(
                "java.lang.NullPointerException\n\tat com.example.Test.main(Test.java:10)");
        testData.setErrorMessage("Test error message");
        testData.setErrorCode("TEST_ERROR");

        clearRedis();
    }

    @AfterEach
    void tearDown() {
        clearRedis();
    }

    private void clearRedis() {
        try {
            stringRedisTemplate.getConnectionFactory()
                    .getConnection()
                    .flushAll();
        } catch (Exception e) {
            try {
                stringRedisTemplate.delete(stringRedisTemplate.keys("*"));
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    void testPersistStackTrace() {
        persistenceService.persistStackTrace(testTraceId, testData);

        assertTrue(persistenceService.existsStackTrace(testTraceId));
    }

    @Test
    void testPersistAndRetrieveStackTrace() {
        persistenceService.persistStackTrace(testTraceId, testData);

        StackTraceData retrievedData =
                persistenceService.retrieveStackTrace(testTraceId);

        assertNotNull(retrievedData);
        assertEquals(testData.getIp(), retrievedData.getIp());
        assertEquals(testData.getErrorMessage(),
                retrievedData.getErrorMessage());
        assertEquals(testData.getErrorCode(),
                retrievedData.getErrorCode());
        assertEquals(testData.getId(), retrievedData.getId());
    }

    @Test
    void testRetrieveNonExistentStackTrace() {
        StackTraceData retrievedData =
                persistenceService.retrieveStackTrace("non-existent-trace");

        assertNull(retrievedData);
    }

    @Test
    void testRetrieveAllStackTraces() {
        String traceId1 = "trace-1";
        String traceId2 = "trace-2";
        String traceId3 = "trace-3";

        persistenceService.persistStackTrace(traceId1, testData);

        StackTraceData testData2 = new StackTraceData();
        testData2.setId(UUID.randomUUID());
        testData2.setIp("192.168.1.2");
        testData2.setTimestamp(LocalDateTime.now());
        testData2.setDate("2024-06-25");
        testData2.setStackTrace(
                "java.lang.RuntimeException\n\tat com.example.Test.main(Test.java:20)");
        testData2.setErrorMessage("Another error");
        testData2.setErrorCode("ANOTHER_ERROR");

        persistenceService.persistStackTrace(traceId2, testData2);

        StackTraceData testData3 = new StackTraceData();
        testData3.setId(UUID.randomUUID());
        testData3.setIp("192.168.1.3");
        testData3.setTimestamp(LocalDateTime.now());
        testData3.setDate("2024-06-25");
        testData3.setStackTrace(
                "java.lang.IOException\n\tat com.example.Test.main(Test.java:30)");
        testData3.setErrorMessage("Third error");
        testData3.setErrorCode("THIRD_ERROR");

        persistenceService.persistStackTrace(traceId3, testData3);

        List<StackTraceData> allTraces =
                persistenceService.retrieveAllStackTraces();

        assertNotNull(allTraces);
        assertEquals(3, allTraces.size());
    }

    @Test
    void testRetrieveStackTracesByIp() {
        String ip = "192.168.1.1";
        testData.setIp(ip);

        persistenceService.persistStackTrace(testTraceId, testData);

        StackTraceData testData2 = new StackTraceData();
        testData2.setId(UUID.randomUUID());
        testData2.setIp("192.168.1.2");
        testData2.setTimestamp(LocalDateTime.now());
        testData2.setDate("2024-06-25");
        testData2.setStackTrace("java.lang.RuntimeException");
        testData2.setErrorMessage("Another error");
        testData2.setErrorCode("ANOTHER_ERROR");

        persistenceService.persistStackTrace("trace-2", testData2);

        List<StackTraceData> tracesByIp =
                persistenceService.retrieveStackTracesByIp(ip);

        assertNotNull(tracesByIp);
        assertEquals(1, tracesByIp.size());
        assertEquals(ip, tracesByIp.get(0).getIp());
    }

    @Test
    void testDeleteStackTrace() {
        persistenceService.persistStackTrace(testTraceId, testData);

        assertTrue(persistenceService.existsStackTrace(testTraceId));

        persistenceService.deleteStackTrace(testTraceId);

        assertFalse(persistenceService.existsStackTrace(testTraceId));
        assertNull(persistenceService.retrieveStackTrace(testTraceId));
    }

    @Test
    void testClearAllStackTraces() {
        persistenceService.persistStackTrace("trace-1", testData);
        persistenceService.persistStackTrace("trace-2", testData);
        persistenceService.persistStackTrace("trace-3", testData);

        assertEquals(3, persistenceService.getStackTraceCount());

        persistenceService.clearAllStackTraces();

        assertEquals(0, persistenceService.getStackTraceCount());
    }

    @Test
    void testGetStackTraceCount() {
        persistenceService.persistStackTrace("trace-1", testData);
        persistenceService.persistStackTrace("trace-2", testData);

        assertEquals(2, persistenceService.getStackTraceCount());
    }

    @Test
    void testGetStackTraceCountEmpty() {
        assertEquals(0, persistenceService.getStackTraceCount());
    }

    @Test
    void testExistsStackTrace() {
        assertFalse(persistenceService.existsStackTrace(testTraceId));

        persistenceService.persistStackTrace(testTraceId, testData);

        assertTrue(persistenceService.existsStackTrace(testTraceId));
    }

    @Test
    void testPersistStackTraceWithTelemetryException() {
        TelemetryException exception =
                new TelemetryException(
                        "Test error occurred",
                        "TEST_EXCEPTION",
                        "java.lang.Exception: Test");

        StackTraceData data =
                new StackTraceData("10.0.0.1", exception);

        persistenceService.persistStackTrace(
                "exception-trace", data);

        StackTraceData retrieved =
                persistenceService.retrieveStackTrace(
                        "exception-trace");

        assertNotNull(retrieved);
        assertEquals("10.0.0.1", retrieved.getIp());
        assertEquals("Test error occurred",
                retrieved.getErrorMessage());
    }
}