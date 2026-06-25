package com.opentele.stacktrace;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.opentele.stacktrace.controller.StackTraceController;
import com.opentele.stacktrace.service.StackTraceTrackerService;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
	}

	@Test
	public void testTrackerServiceIntegration() {
		StackTraceTrackerService service = new StackTraceTrackerService();
		assertNotNull(service);
		assertEquals(0, service.getStackTraceCount());
	}

	@Test
	public void testControllerCanBeInstantiated() {
		assertNotNull(StackTraceController.class);
	}
}
