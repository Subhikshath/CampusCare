package com.CampusCare.department;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/departments")
@RequiredArgsConstructor
public class DepartmentController
{

	private final DepartmentService departmentService;

	@PostMapping
	public ResponseEntity<DepartmentResponse> create(
			@Valid @RequestBody DepartmentRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(departmentService.create(request));
	}

	@GetMapping
	public ResponseEntity<List<DepartmentResponse>> findAll() {
		return ResponseEntity.ok(departmentService.findAll());
	}

	@PutMapping("/{id}")
	public ResponseEntity<DepartmentResponse> update(
			@PathVariable Long id,
			@Valid @RequestBody DepartmentRequest request) {
		return ResponseEntity.ok(departmentService.update(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> disable(@PathVariable Long id) {
		departmentService.disable(id);
		return ResponseEntity.noContent().build();
	}
}
