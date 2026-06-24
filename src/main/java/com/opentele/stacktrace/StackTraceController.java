package com.opentele.stacktrace;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
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

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import jakarta.servlet.http.HttpServletRequest;

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
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "OpenTelemetry Stacktrace Tracker is running");
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/documentation", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getDocumentation() throws IOException {
        Resource resource = new ClassPathResource("how.html");
        String content = new String(
                Files.readAllBytes(resource.getFile().toPath()),
                StandardCharsets.UTF_8
        );
        return ResponseEntity.ok(content);
    }

    @PostMapping("/track-error")
    public ResponseEntity<Map<String, Object>> trackError(
            @RequestParam String message,
            HttpServletRequest request) {

        String ip = extractClientIp(request);

        TelemetryException exception = new TelemetryException(
                message,
                "java.sql.SQLException",
                """
                java.sql.SQLException: Connection refused
                    at com.demo.DatabaseService.connect(DatabaseService.java:25)
                    at com.demo.UserService.loadUser(UserService.java:42)
                    at com.demo.Application.main(Application.java:10)
                """
        );

        StackTraceData data = trackerService.trackStackTrace(ip, exception);

        // ❌ DO NOT do this:
        // exception.setStackTrace(null);

        // ✅ If you REALLY want to clear it, use:
        // exception.setStackTrace(new StackTraceElement[0]);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "tracked");
        response.put("ip", ip);
        response.put("timestamp", data.getTimestamp());
        response.put("date", data.getDate());
        response.put("errorMessage", data.getErrorMessage());
        response.put("errorType", data.getErrorType());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/tracked-stacktraces")
    public ResponseEntity<Map<String, Object>> getTrackedStackTraces() {
        Map<String, Object> response = new HashMap<>();
        response.put("count", trackerService.getStackTraceCount());
        response.put("stacktraces", trackerService.getAllTrackedStackTraces());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tracked-stacktraces/ip/{ip}")
    public ResponseEntity<Map<String, Object>> getStackTracesByIp(@PathVariable String ip) {
        Map<String, Object> response = new HashMap<>();
        response.put("ip", ip);
        response.put("stacktraces", trackerService.getStackTracesByIp(ip));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear-stacktraces")
    public ResponseEntity<Map<String, String>> clearStackTraces() {
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
}