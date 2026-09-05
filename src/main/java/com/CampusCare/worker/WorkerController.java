package com.CampusCare.worker;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/workers")
@RequiredArgsConstructor
public class WorkerController {

	private final WorkerService workerService;

	@PostMapping
	public ResponseEntity<WorkerAssignment> create(
			@Valid @RequestBody WorkerRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(workerService.create(request));
	}

	@GetMapping
	public ResponseEntity<List<WorkerAssignment>> findAll() {
		return ResponseEntity.ok(workerService.findAll());
	}
}
