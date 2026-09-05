package com.CampusCare.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	@GetMapping
	public ResponseEntity<List<NotificationResponse>> findMine(Authentication authentication) {
		return ResponseEntity.ok(notificationService.findMine(authentication.getName()));
	}

	@PutMapping("/{id}/read")
	public ResponseEntity<Void> markRead(
			@PathVariable Long id, Authentication authentication) {
		notificationService.markRead(authentication.getName(), id);
		return ResponseEntity.noContent().build();
	}
}
