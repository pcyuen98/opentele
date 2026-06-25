package com.opentele.stacktrace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(EmbeddedRedisConfiguration.class)
class RedisStackTracePersistenceServiceTest {

    @Autowired
    private RedisStackTracePersistenceService persistenceService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private StackTraceData testData;
    private String testTraceId;

    @BeforeEach
    void setUp() {
        testTraceId = "trace-" + UUID.randomUUID().toString();
        
        testData = new StackTraceData();
        testData.setId(UUID.randomUUID());
        testData.setIp("192.168.1.1");
        testData.setTimestamp(LocalDateTime.now());
        testData.setDate("2024-06-25");
        testData.setStackTrace("java.lang.NullPointerException\n\tat com.example.Test.main(Test.java:10)");
        testData.setErrorMessage("Test error message");
        testData.setErrorCode("TEST_ERROR");

        // Clear any existing data
        try {
            stringRedisTemplate.getConnectionFactory().getConnection().flushAll();
        } catch (Exception e) {
            // Fallback: clear using keys pattern
            try {
                stringRedisTemplate.delete(stringRedisTemplate.keys("*"));
            } catch (Exception ignored) {
            }
        }
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        try {
            stringRedisTemplate.getConnectionFactory().getConnection().flushAll();
        } catch (Exception e) {
            // Fallback: clear using keys pattern
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

        StackTraceData retrievedData = persistenceService.retrieveStackTrace(testTraceId);

        assertNotNull(retrievedData);
        assertEquals(testData.getIp(), retrievedData.getIp());
        assertEquals(testData.getErrorMessage(), retrievedData.getErrorMessage());
        assertEquals(testData.getErrorCode(), retrievedData.getErrorCode());
        assertEquals(testData.getId(), retrievedData.getId());
    }

    @Test
    void testRetrieveNonExistentStackTrace() {
        StackTraceData retrievedData = persistenceService.retrieveStackTrace("non-existent-trace");

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
        testData2.setStackTrace("java.lang.RuntimeException\n\tat com.example.Test.main(Test.java:20)");
        testData2.setErrorMessage("Another error");
        testData2.setErrorCode("ANOTHER_ERROR");

        persistenceService.persistStackTrace(traceId2, testData2);

        StackTraceData testData3 = new StackTraceData();
        testData3.setId(UUID.randomUUID());
        testData3.setIp("192.168.1.3");
        testData3.setTimestamp(LocalDateTime.now());
        testData3.setDate("2024-06-25");
        testData3.setStackTrace("java.lang.IOException\n\tat com.example.Test.main(Test.java:30)");
        testData3.setErrorMessage("Third error");
        testData3.setErrorCode("THIRD_ERROR");

        persistenceService.persistStackTrace(traceId3, testData3);

        List<StackTraceData> allTraces = persistenceService.retrieveAllStackTraces();

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

        List<StackTraceData> tracesByIp = persistenceService.retrieveStackTracesByIp(ip);

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

        long countBefore = persistenceService.getStackTraceCount();
        assertEquals(3, countBefore);

        persistenceService.clearAllStackTraces();

        long countAfter = persistenceService.getStackTraceCount();
        assertEquals(0, countAfter);
    }

    @Test
    void testGetStackTraceCount() {
        persistenceService.persistStackTrace("trace-1", testData);
        persistenceService.persistStackTrace("trace-2", testData);

        long count = persistenceService.getStackTraceCount();

        assertEquals(2, count);
    }

    @Test
    void testGetStackTraceCountEmpty() {
        long count = persistenceService.getStackTraceCount();

        assertEquals(0, count);
    }

    @Test
    void testExistsStackTrace() {
        assertFalse(persistenceService.existsStackTrace(testTraceId));

        persistenceService.persistStackTrace(testTraceId, testData);

        assertTrue(persistenceService.existsStackTrace(testTraceId));
    }

    @Test
    void testPersistMultipleStackTracesForSameIp() {
        String ip = "192.168.1.100";
        testData.setIp(ip);

        persistenceService.persistStackTrace("trace-1", testData);

        StackTraceData testData2 = new StackTraceData();
        testData2.setId(UUID.randomUUID());
        testData2.setIp(ip);
        testData2.setTimestamp(LocalDateTime.now());
        testData2.setDate("2024-06-25");
        testData2.setStackTrace("java.lang.Exception");
        testData2.setErrorMessage("Error 2");
        testData2.setErrorCode("ERROR_2");

        persistenceService.persistStackTrace("trace-2", testData2);

        List<StackTraceData> tracesByIp = persistenceService.retrieveStackTracesByIp(ip);

        assertNotNull(tracesByIp);
        assertEquals(2, tracesByIp.size());
        assertTrue(tracesByIp.stream().allMatch(t -> t.getIp().equals(ip)));
    }

    @Test
    void testDeleteStackTraceRemovesFromIpIndex() {
        String ip = "192.168.1.50";
        testData.setIp(ip);
        
        persistenceService.persistStackTrace(testTraceId, testData);
        
        List<StackTraceData> tracesByIpBefore = persistenceService.retrieveStackTracesByIp(ip);
        assertEquals(1, tracesByIpBefore.size());

        persistenceService.deleteStackTrace(testTraceId);

        List<StackTraceData> tracesByIpAfter = persistenceService.retrieveStackTracesByIp(ip);
        assertEquals(0, tracesByIpAfter.size());
    }

    @Test
    void testPersistStackTraceWithTelemetryException() {
        String message = "Test error occurred";
        String errorType = "TEST_EXCEPTION";
        String stackTrace = "java.lang.Exception: Test\n\tat com.example.Test.main(Test.java:1)";
        
        TelemetryException exception = new TelemetryException(message, errorType, stackTrace);
        StackTraceData dataFromException = new StackTraceData("10.0.0.1", exception);

        persistenceService.persistStackTrace("exception-trace", dataFromException);

        StackTraceData retrieved = persistenceService.retrieveStackTrace("exception-trace");

        assertNotNull(retrieved);
        assertEquals("10.0.0.1", retrieved.getIp());
        assertEquals(message, retrieved.getErrorMessage());
    }

    @Test
    void testDataPersistenceWithJsonSerialization() throws Exception {
        persistenceService.persistStackTrace(testTraceId, testData);

        StackTraceData retrieved = persistenceService.retrieveStackTrace(testTraceId);

        assertNotNull(retrieved);
        assertEquals(testData.getId(), retrieved.getId());
        assertEquals(testData.getIp(), retrieved.getIp());
        assertEquals(testData.getErrorCode(), retrieved.getErrorCode());
        assertEquals(testData.getErrorMessage(), retrieved.getErrorMessage());
        assertEquals(testData.getStackTrace(), retrieved.getStackTrace());
        assertNotNull(retrieved.getTimestamp());
    }

    @Test
    void testEmptyIpStackTraces() {
        List<StackTraceData> emptyTraces = persistenceService.retrieveStackTracesByIp("non-existent-ip");

        assertNotNull(emptyTraces);
        assertEquals(0, emptyTraces.size());
    }

    @Test
    void testClearAllStackTracesWithMultipleIps() {
        testData.setIp("192.168.1.1");
        persistenceService.persistStackTrace("trace-1", testData);

        testData.setIp("192.168.1.2");
        persistenceService.persistStackTrace("trace-2", testData);

        testData.setIp("192.168.1.3");
        persistenceService.persistStackTrace("trace-3", testData);

        assertEquals(3, persistenceService.getStackTraceCount());

        persistenceService.clearAllStackTraces();

        assertEquals(0, persistenceService.getStackTraceCount());
        assertEquals(0, persistenceService.retrieveStackTracesByIp("192.168.1.1").size());
        assertEquals(0, persistenceService.retrieveStackTracesByIp("192.168.1.2").size());
        assertEquals(0, persistenceService.retrieveStackTracesByIp("192.168.1.3").size());
    }
}
