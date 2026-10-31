package com.rishi.venue.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(@Autowired(required = false) DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("timestamp", Instant.now().toString());
        response.put("application", "venue-booking");

        boolean dbHealthy = true;
        String dbDetails = "connected";

        if (dataSource != null) {
            try (Connection conn = dataSource.getConnection()) {
                if (!conn.isValid(3)) {
                    dbHealthy = false;
                    dbDetails = "connection validation failed";
                }
            } catch (Exception ex) {
                dbHealthy = false;
                dbDetails = "unreachable: " + ex.getMessage();
            }
        }

        Map<String, Object> components = new LinkedHashMap<>();
        Map<String, Object> dbComponent = new LinkedHashMap<>();
        dbComponent.put("status", dbHealthy ? "UP" : "DOWN");
        dbComponent.put("details", dbDetails);
        components.put("database", dbComponent);

        response.put("components", components);

        if (!dbHealthy) {
            response.put("status", "DOWN");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }

        return ResponseEntity.ok(response);
    }
}
