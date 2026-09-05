package com.CampusCare.department;

import com.CampusCare.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}