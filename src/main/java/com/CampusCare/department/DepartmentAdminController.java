package com.CampusCare.department;

import com.CampusCare.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/department-admins")
@RequiredArgsConstructor
public class DepartmentAdminController {

    private final DepartmentAdminService departmentAdminService;

    @PostMapping
    public ResponseEntity<DepartmentAdminResponse> createDepartmentAdmin(
            @Valid @RequestBody DepartmentAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(departmentAdminService.createDepartmentAdmin(request));
    }

    @GetMapping
    public ResponseEntity<List<DepartmentAdminResponse>> findDepartmentAdmins() {
        return ResponseEntity.ok(departmentAdminService.findDepartmentAdmins());
    }

    @PutMapping("/{id}/departments")
    public ResponseEntity<DepartmentAdminResponse> updateDepartments(
            @PathVariable Long id, @RequestBody List<Long> departmentIds) {
        return ResponseEntity.ok(departmentAdminService.updateDepartments(id, departmentIds));
    }
}