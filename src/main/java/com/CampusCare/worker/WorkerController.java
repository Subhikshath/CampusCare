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

	@PutMapping("/{id}/departments")
	public ResponseEntity<WorkerAssignment> updateDepartments(
			@PathVariable Long id, @RequestBody List<Long> departmentIds) {
		return ResponseEntity.ok(workerService.updateDepartments(id, departmentIds));
	}

	@PutMapping("/{id}/enabled")
	public ResponseEntity<WorkerAssignment> setEnabled(
			@PathVariable Long id, @RequestParam boolean enabled) {
		return ResponseEntity.ok(workerService.setEnabled(id, enabled));
	}
}
