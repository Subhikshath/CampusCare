package com.CampusCare.worker;

import com.CampusCare.complaint.ComplaintResponse;
import com.CampusCare.complaint.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/worker/complaints")
@RequiredArgsConstructor
public class WorkerComplaintController {

    private final ComplaintService complaintService;

    @GetMapping
    public ResponseEntity<List<ComplaintResponse>> findMine(Authentication authentication) {
        return ResponseEntity.ok(complaintService.findWorkerComplaints(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplaintResponse> findOne(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(complaintService.findWorkerComplaint(
                authentication.getName(), id));
    }
}
