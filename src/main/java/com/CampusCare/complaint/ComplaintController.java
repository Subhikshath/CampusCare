package com.CampusCare.complaint;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
			public ResponseEntity<ComplaintPageResponse> findMine(
			    Authentication authentication,
			    @RequestParam(required = false) ComplaintStatus status,
			    @RequestParam(required = false) Long departmentId,
			    @RequestParam(required = false) String search,
			    @RequestParam(required = false) LocalDate fromDate,
			    @RequestParam(required = false) LocalDate toDate,
			    @RequestParam(required = false) String group,
			    @RequestParam(required = false) Boolean workerAssigned,
			    @RequestParam(defaultValue = "0") int page,
			    @RequestParam(defaultValue = "20") int size,
			    @RequestParam(defaultValue = "createdAt,desc") String sort) {
			String[] sorting = sort.split(",", 2);
			Sort.Direction direction = sorting.length > 1
				&& sorting[1].equalsIgnoreCase("asc")
				? Sort.Direction.ASC : Sort.Direction.DESC;
			Pageable pageable = PageRequest.of(page, Math.min(size, 100),
				Sort.by(direction, sorting[0]));
			Page<ComplaintResponse> pageResult = complaintService.findStudentComplaints(
					authentication.getName(), status, departmentId, search,
					fromDate, toDate, group, workerAssigned, pageable);
			return ResponseEntity.ok(ComplaintPageResponse.from(pageResult));
		    }

		    @GetMapping("/{id}")
		    public ResponseEntity<ComplaintDetailResponse> findOne(
			    @PathVariable Long id, Authentication authentication) {
			return ResponseEntity.ok(complaintService.findStudentComplaintDetails(
				authentication.getName(), id));
		    }

		    @GetMapping("/{id}/history")
			public ResponseEntity<List<ComplaintHistoryResponse>> history(
			    @PathVariable Long id, Authentication authentication) {
			return ResponseEntity.ok(complaintService.findHistory(
				authentication.getName(), id));
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
