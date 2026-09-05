package com.CampusCare.worker;

import com.CampusCare.department.Department;
import com.CampusCare.department.DepartmentRepository;
import com.CampusCare.user.User;
import com.CampusCare.user.UserRepository;
import com.CampusCare.user.UserRole;
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
            throw new IllegalArgumentException("College email already exists");
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

    private WorkerAssignment toResponse(User worker) {
        return WorkerAssignment.builder()
                .userId(worker.getId())
                .name(worker.getName())
                .collegeEmail(worker.getCollegeEmail())
                .departmentIds(worker.getDepartments().stream()
                        .map(Department::getId)
                        .toList())
                .build();
    }
}
