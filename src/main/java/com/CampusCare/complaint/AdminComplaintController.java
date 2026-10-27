package com.CampusCare.complaint;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/complaints")
@RequiredArgsConstructor
public class AdminComplaintController {

    private final ComplaintService complaintService;

    @GetMapping
    public ResponseEntity<List<ComplaintResponse>> findAll() {
        return ResponseEntity.ok(complaintService.findAllForAdmin());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplaintDetailResponse> findOne(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(complaintService.findAdminComplaintDetails(
                authentication.getName(), id));
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