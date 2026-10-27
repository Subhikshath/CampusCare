package com.CampusCare.department;

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
public class DepartmentAdminService {

    private final UserRepository userRepository;

    private final DepartmentRepository departmentRepository;

    private final PasswordEncoder passwordEncoder;

    @Transactional
        public DepartmentAdminResponse createDepartmentAdmin(
            DepartmentAdminRequest request) {

                if (request.getDepartmentIds() == null
                                || request.getDepartmentIds().isEmpty()
                                || request.getDepartmentIds().size() > 2) {
                        throw new IllegalArgumentException(
                                        "A department admin must manage between 1 and 2 departments");
                }

                if (new HashSet<>(request.getDepartmentIds()).size()
                                != request.getDepartmentIds().size()) {
                        throw new IllegalArgumentException("Duplicate department IDs are not allowed");
                }

        if (userRepository.existsByCollegeEmail(
                request.getCollegeEmail())) {

            throw new DuplicateResourceException(
                    "College email already exists"
            );
        }

        List<Department> departments =
                departmentRepository.findAllById(
                        request.getDepartmentIds()
                );

        if (departments.size() !=
                request.getDepartmentIds().size()) {

            throw new ResourceNotFoundException(
                    "One or more departments not found"
            );
        }
                if (departments.stream().anyMatch(department -> !department.isEnabled())) {
                        throw new IllegalArgumentException("All departments must be enabled");
                }

        User user = User.builder()
                .name(request.getName())
                .collegeEmail(request.getCollegeEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.DEPARTMENT_ADMIN)
                .enabled(true)
                .departments(new HashSet<>(departments))
                .build();

        User savedUser = userRepository.save(user);

                return DepartmentAdminResponse.from(savedUser);
    }

        @Transactional(readOnly = true)
        public List<DepartmentAdminResponse> findDepartmentAdmins() {
                return userRepository.findByRole(UserRole.DEPARTMENT_ADMIN).stream()
                                .map(DepartmentAdminResponse::from)
                                .toList();
        }

        @Transactional
        public DepartmentAdminResponse updateDepartments(Long userId, List<Long> departmentIds) {
                if (departmentIds == null || departmentIds.isEmpty() || departmentIds.size() > 2
                                || new HashSet<>(departmentIds).size() != departmentIds.size()) {
                        throw new IllegalArgumentException("A department admin must manage between 1 and 2 unique departments");
                }
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("Department admin not found"));
                if (user.getRole() != UserRole.DEPARTMENT_ADMIN) {
                        throw new IllegalArgumentException("User is not a department admin");
                }
                List<Department> departments = departmentRepository.findAllById(departmentIds);
                if (departments.size() != departmentIds.size()
                                || departments.stream().anyMatch(department -> !department.isEnabled())) {
                        throw new IllegalArgumentException("All departments must exist and be enabled");
                }
                user.setDepartments(new HashSet<>(departments));
                return DepartmentAdminResponse.from(userRepository.save(user));
        }
}