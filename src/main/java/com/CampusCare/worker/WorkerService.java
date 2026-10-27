package com.CampusCare.worker;

import com.CampusCare.department.Department;
import com.CampusCare.department.DepartmentRepository;
import com.CampusCare.user.User;
import com.CampusCare.user.UserRepository;
import com.CampusCare.user.UserRole;
import com.CampusCare.common.DuplicateResourceException;
import com.CampusCare.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkerService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public WorkerAssignment create(WorkerRequest request) {
        if (userRepository.existsByCollegeEmail(request.getCollegeEmail())) {
            throw new DuplicateResourceException("College email already exists");
        }

        if (new HashSet<>(request.getDepartmentIds()).size()
                != request.getDepartmentIds().size()) {
            throw new IllegalArgumentException("Duplicate department IDs are not allowed");
        }

        List<Department> departments = departmentRepository.findAllById(request.getDepartmentIds());
        if (departments.size() != request.getDepartmentIds().size()
                || departments.stream().anyMatch(department -> !department.isEnabled())) {
            throw new IllegalArgumentException("All departments must exist and be enabled");
        }

        User worker = User.builder()
                .name(request.getName().trim())
                .collegeEmail(request.getCollegeEmail().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.WORKER)
                .enabled(true)
                .departments(new HashSet<>(departments))
                .build();

        return toResponse(userRepository.save(worker));
    }

    @Transactional(readOnly = true)
    public List<WorkerAssignment> findAll() {
        return userRepository.findByRole(UserRole.WORKER).stream()
                .map(this::toResponse)
                .toList();
    }

            @Transactional(readOnly = true)
            public List<WorkerAssignment> findForDepartmentAdmin(String adminEmail) {
            User admin = userRepository.findByCollegeEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Department admin not found"));
            if (admin.getRole() != UserRole.DEPARTMENT_ADMIN) {
                throw new IllegalArgumentException("User is not a department admin");
            }
            var departmentIds = admin.getDepartments().stream()
                .map(Department::getId)
                .collect(java.util.stream.Collectors.toSet());
            return userRepository.findByRole(UserRole.WORKER).stream()
                .filter(worker -> worker.getDepartments().stream()
                    .anyMatch(department -> departmentIds.contains(department.getId())))
                .map(this::toResponse)
                .toList();
            }

    @Transactional
    public WorkerAssignment updateDepartments(Long workerId, List<Long> departmentIds) {
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found"));
        if (worker.getRole() != UserRole.WORKER) {
            throw new IllegalArgumentException("User is not a worker");
        }
        if (departmentIds == null || departmentIds.isEmpty()
                || new HashSet<>(departmentIds).size() != departmentIds.size()) {
            throw new IllegalArgumentException("At least one unique department is required");
        }
        List<Department> departments = departmentRepository.findAllById(departmentIds);
        if (departments.size() != departmentIds.size()
                || departments.stream().anyMatch(department -> !department.isEnabled())) {
            throw new IllegalArgumentException("All departments must exist and be enabled");
        }
        worker.setDepartments(new HashSet<>(departments));
        return toResponse(userRepository.save(worker));
    }

    @Transactional
    public WorkerAssignment setEnabled(Long workerId, boolean enabled) {
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found"));
        if (worker.getRole() != UserRole.WORKER) {
            throw new IllegalArgumentException("User is not a worker");
        }
        worker.setEnabled(enabled);
        return toResponse(userRepository.save(worker));
    }

    private WorkerAssignment toResponse(User worker) {
        return WorkerAssignment.builder()
                .userId(worker.getId())
                .name(worker.getName())
                .collegeEmail(worker.getCollegeEmail())
				.enabled(worker.isEnabled())
                .departmentIds(worker.getDepartments().stream()
                        .map(Department::getId)
                        .toList())
                .build();
    }
}
