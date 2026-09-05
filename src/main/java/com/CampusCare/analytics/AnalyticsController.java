package com.CampusCare.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

	private final AnalyticsService analyticsService;

	@GetMapping
	public ResponseEntity<Map<String, Object>> summary() {
		return ResponseEntity.ok(analyticsService.summary());
	}
}
