package com.CampusCare.complaint;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/department-admin/complaints")
@RequiredArgsConstructor
public class DepartmentComplaintController {

    private final ComplaintService complaintService;

    @GetMapping
    public ResponseEntity<List<ComplaintResponse>> findAll(Authentication authentication) {
        return ResponseEntity.ok(complaintService.findDepartmentComplaints(
                authentication.getName()));
    }

        @GetMapping("/{id}")
        public ResponseEntity<ComplaintDetailResponse> findOne(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(complaintService.findDepartmentComplaintDetails(
            authentication.getName(), id));
        }

        @GetMapping("/{id}/history")
        public ResponseEntity<List<ComplaintHistoryResponse>> history(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(complaintService.findDepartmentHistory(
            authentication.getName(), id));
        }

    @PutMapping("/{id}/worker")
    public ResponseEntity<ComplaintResponse> assignWorker(
            @PathVariable Long id,
            @Valid @RequestBody WorkerAssignmentRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(complaintService.assignWorker(
                authentication.getName(), id, request.getWorkerId()));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ComplaintResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(complaintService.updateStatus(
                authentication.getName(), id, request.getStatus()));
    }
}