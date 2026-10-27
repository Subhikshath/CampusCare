package com.CampusCare.worker;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/department-admin/workers")
@RequiredArgsConstructor
public class DepartmentWorkerController {

    private final WorkerService workerService;

    @GetMapping
    public ResponseEntity<List<WorkerAssignment>> findForDepartment(Authentication authentication) {
        return ResponseEntity.ok(workerService.findForDepartmentAdmin(authentication.getName()));
    }
}