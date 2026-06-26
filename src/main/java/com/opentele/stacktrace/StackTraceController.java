package com.opentele.stacktrace;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.opentele.stacktrace.exception.TelemetryException;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OpenAPIDefinition(
        info = @Info(
                title = "OpenTelemetry Stacktrace Tracker API",
                version = "1.0.0",
                description = "API for tracking and persisting stack traces with OpenTelemetry using Redis"
        )
)
@RestController
@RequestMapping("/api")
public class StackTraceController {

    @Autowired
    private StackTraceTrackerService trackerService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        log.debug("Health check endpoint called");
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "OpenTelemetry Stacktrace Tracker is running");
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/documentation", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getDocumentation() throws IOException {
        log.debug("Documentation endpoint called");
        Resource resource = new ClassPathResource("how.html");
        String content = new String(
                Files.readAllBytes(resource.getFile().toPath()),
                StandardCharsets.UTF_8
        );
        return ResponseEntity.ok(content);
    }

	@PostMapping("/track-error")
	public ResponseEntity<Map<String, Object>> trackError(
			@RequestParam String message, String errorCode, String stacktrace,
			HttpServletRequest request) {

		String ip = extractClientIp(request);

		TelemetryException exception = new TelemetryException(
				message,
				errorCode,
				stacktrace
		);

		StackTraceData data = trackerService.trackStackTrace(ip, exception);

		Map<String, Object> response = new HashMap<>();
		response.put("status", "tracked");
		response.put("ip", ip);
		response.put("timestamp", data.getTimestamp());
		response.put("date", data.getDate());
		response.put("errorMessage", data.getErrorMessage());
		response.put("errorCode", data.getErrorCode());

		return ResponseEntity.ok(response);
	}

    @GetMapping("/tracked-stacktraces")
    public ResponseEntity<Map<String, Object>> getTrackedStackTraces() {
        log.debug("Fetching all tracked stack traces");
        Map<String, Object> response = new HashMap<>();
        response.put("count", trackerService.getStackTraceCount());
        response.put("stacktraces", trackerService.getAllTrackedStackTraces());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tracked-stacktraces/ip/{ip}")
    public ResponseEntity<Map<String, Object>> getStackTracesByIp(@PathVariable String ip) {
        log.debug("Fetching stack traces for IP: {}", ip);
        Map<String, Object> response = new HashMap<>();
        response.put("ip", ip);
        response.put("stacktraces", trackerService.getStackTracesByIp(ip));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear-stacktraces")
    public ResponseEntity<Map<String, String>> clearStackTraces() {
        log.info("Clearing all tracked stack traces");
        trackerService.clearTrackedStackTraces();

        Map<String, String> response = new HashMap<>();
        response.put("status", "cleared");
        response.put("message", "All tracked stacktraces have been cleared from Redis");
        return ResponseEntity.ok(response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
    
    @GetMapping("/tracked-stacktraces-filter")
    public ResponseEntity<Map<String, Object>> filterStackTraces(
            @RequestParam(required = false) String fromTimestamp,
            @RequestParam(required = false) String toTimestamp,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String errorCode,
            @RequestParam(required = false) String messageSearch) {

        LocalDateTime from = null;
        LocalDateTime to = null;

        DateTimeFormatter formatter =
                DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        if (fromTimestamp != null && !fromTimestamp.isBlank()) {
            from = LocalDateTime.parse(fromTimestamp, formatter);
        }

        if (toTimestamp != null && !toTimestamp.isBlank()) {
            to = LocalDateTime.parse(toTimestamp, formatter);
        }

        List<StackTraceData> results =
                trackerService.filterStackTraces(
                        from,
                        to,
                        ip,
                        errorCode,
                        messageSearch);

        Map<String, Object> response = new HashMap<>();
        response.put("count", results.size());
        response.put("stacktraces", results);

        Map<String, Object> filters = new HashMap<>();
        filters.put("fromTimestamp", fromTimestamp);
        filters.put("toTimestamp", toTimestamp);
        filters.put("ip", ip);
        filters.put("errorCode", errorCode);
        filters.put("messageSearch", messageSearch);

        response.put("filters", filters);

        return ResponseEntity.ok(response);
    }
}