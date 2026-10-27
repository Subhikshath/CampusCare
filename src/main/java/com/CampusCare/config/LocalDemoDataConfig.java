package com.CampusCare.config;

import com.CampusCare.department.Department;
import com.CampusCare.department.DepartmentRepository;
import com.CampusCare.department.DepartmentType;
import com.CampusCare.user.User;
import com.CampusCare.user.UserRepository;
import com.CampusCare.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;

@Configuration
@Profile("local")
@RequiredArgsConstructor
public class LocalDemoDataConfig {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public org.springframework.boot.CommandLineRunner seedLocalDemoData() {
        return args -> {
            for (DepartmentType type : DepartmentType.values()) {
                departmentRepository.findByCode(type.name())
                        .orElseGet(() -> departmentRepository.save(Department.builder()
                                .name(type.getDisplayName())
                                .code(type.name())
                                .enabled(true)
                                .build()));
            }
            Department plumbing = departmentRepository.findByCode(DepartmentType.PLUMBING.name())
                    .orElseThrow(() -> new IllegalStateException("Plumbing department was not seeded"));
            Department electrical = departmentRepository
                    .findByCode(DepartmentType.ELECTRICAL_MAINTENANCE.name())
                    .orElseThrow(() -> new IllegalStateException(
                            "Electrical Maintenance department was not seeded"));

            createUser("Main Admin", "admin@college.edu", "admin12345", UserRole.ADMIN, null, List.of());
            createUser("Harini R", "student@college.edu", "student123", UserRole.STUDENT, "23IS008", List.of());
            createUser("Facilities Admin", "department-admin@college.edu", "admin12345", UserRole.DEPARTMENT_ADMIN, null, List.of(plumbing, electrical));
            createUser("Maintenance Worker", "worker@college.edu", "worker123", UserRole.WORKER, null, List.of(plumbing, electrical));
        };
    }

    private void createUser(String name, String email, String password, UserRole role,
                            String rollNo, List<Department> departments) {
        if (userRepository.findByCollegeEmail(email).isPresent()) {
            return;
        }
        userRepository.save(User.builder()
                .name(name)
                .collegeEmail(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .rollNo(rollNo)
                .enabled(true)
                .departments(new HashSet<>(departments))
                .build());
    }
}