package com.CampusCare.complaint;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/student/complaints")
@RequiredArgsConstructor
public class ComplaintController {

	private final ComplaintService complaintService;

	@PostMapping
	public ResponseEntity<ComplaintResponse> create(
			@Valid @RequestBody ComplaintRequest request,
			Authentication authentication) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(complaintService.create(authentication.getName(), request));
	}

		    @GetMapping
		    public ResponseEntity<Page<ComplaintResponse>> findMine(
			    Authentication authentication,
			    @RequestParam(required = false) ComplaintStatus status,
			    @RequestParam(required = false) Long departmentId,
			    @RequestParam(required = false) String search,
			    @RequestParam(required = false) LocalDate fromDate,
			    @RequestParam(required = false) LocalDate toDate,
			    @RequestParam(defaultValue = "0") int page,
			    @RequestParam(defaultValue = "20") int size,
			    @RequestParam(defaultValue = "createdAt,desc") String sort) {
			String[] sorting = sort.split(",", 2);
			Sort.Direction direction = sorting.length > 1
				&& sorting[1].equalsIgnoreCase("asc")
				? Sort.Direction.ASC : Sort.Direction.DESC;
			Pageable pageable = PageRequest.of(page, Math.min(size, 100),
				Sort.by(direction, sorting[0]));
			return ResponseEntity.ok(complaintService.findStudentComplaints(
				authentication.getName(), status, departmentId, search,
				fromDate, toDate, pageable));
		    }

		    @GetMapping("/{id}")
		    public ResponseEntity<ComplaintResponse> findOne(
			    @PathVariable Long id, Authentication authentication) {
			return ResponseEntity.ok(complaintService.findStudentComplaint(
				authentication.getName(), id));
		    }

		    @GetMapping("/{id}/history")
		    public ResponseEntity<List<ComplaintHistory>> history(
			    @PathVariable Long id, Authentication authentication) {
			return ResponseEntity.ok(complaintService.findHistory(
				authentication.getName(), id));
		    }

	@PostMapping("/{id}/images")
	public ResponseEntity<String> addImage(
			@PathVariable Long id,
			@RequestPart("file") MultipartFile file,
			Authentication authentication) {
		return ResponseEntity.status(HttpStatus.CREATED).body(
				complaintService.addImage(authentication.getName(), id, file));
	}

	@PostMapping("/{id}/follow-up")
	public ResponseEntity<ComplaintResponse> answerFollowUp(
			@PathVariable Long id,
			@Valid @RequestBody FollowUpRequest request,
			Authentication authentication) {
		return ResponseEntity.ok(complaintService.answerFollowUp(
				authentication.getName(), id, request));
	}
}
