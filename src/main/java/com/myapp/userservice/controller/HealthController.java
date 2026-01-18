package com.myapp.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "Health", description = "Health check endpoints")
public class HealthController {

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;

    public HealthController(DataSource dataSource, StringRedisTemplate redisTemplate) {
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check endpoint")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> services = new HashMap<>();

        boolean healthy = true;
        String errorMessage = null;

        // Check database
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute("SELECT 1");
            services.put("database", "connected");
        } catch (Exception e) {
            services.put("database", "disconnected");
            healthy = false;
            errorMessage = "Database: " + e.getMessage();
        }

        // Check Redis
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            services.put("redis", "connected");
        } catch (Exception e) {
            services.put("redis", "disconnected");
            healthy = false;
            errorMessage = (errorMessage != null ? errorMessage + "; " : "") + "Redis: " + e.getMessage();
        }

        response.put("success", healthy);
        response.put("message", healthy ? "Service is healthy" : "Service is unhealthy");
        response.put("timestamp", Instant.now().toString());
        response.put("services", services);

        if (!healthy && errorMessage != null) {
            response.put("error", errorMessage);
        }

        return ResponseEntity.status(healthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
