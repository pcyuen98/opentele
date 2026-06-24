package com.opentele.stacktrace;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
public class ApplicationTest {

	@Test
	public void contextLoads() {
		assertNotNull(ApplicationTest.class);
	}

	@Test
	public void applicationStartsSuccessfully() {
		assertNotNull(ApplicationTest.class);
	}
}

