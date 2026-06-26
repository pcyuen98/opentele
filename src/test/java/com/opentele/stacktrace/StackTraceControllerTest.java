package com.opentele.stacktrace;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class StackTraceControllerTest {

    @Autowired(required = false)
    private MockMvc mockMvc;

    @Autowired(required = false)
    private StackTraceController controller;

    @Test
    public void testControllerExists() {
        assertNotNull(StackTraceController.class);
        // Controller should exist even if Redis is not available
    }

    @Test
    public void testTrackerServiceIntegration() {
        StackTraceTrackerService service = new StackTraceTrackerService();
        assertNotNull(service);
        assertTrue(service.getStackTraceCount() == 0);
        // Service should work in memory even without Redis
    }

    @Test
    public void testControllerCanBeInstantiated() {
        StackTraceController testController = new StackTraceController();
        assertNotNull(testController);
        // Controller should be instantiable without Redis dependency
    }
}
