package com.CampusCare.department;

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

            throw new RuntimeException(
                    "College email already exists"
            );
        }

        List<Department> departments =
                departmentRepository.findAllById(
                        request.getDepartmentIds()
                );

        if (departments.size() !=
                request.getDepartmentIds().size()) {

            throw new RuntimeException(
                    "One or more departments not found"
            );
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
}