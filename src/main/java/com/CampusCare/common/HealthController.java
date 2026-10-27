package com.CampusCare.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping({"/", "/health"})
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "application", "CampusCare AI",
                "status", "UP",
                "message", "Backend is running. Use /auth/login to authenticate."
        ));
    }
}
