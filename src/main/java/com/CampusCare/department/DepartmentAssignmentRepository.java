package com.CampusCare.department;

import com.CampusCare.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentAssignmentRepository
        extends JpaRepository<DepartmentAssignment, Long> {

    boolean existsByUserIdAndDepartmentId(
            Long userId,
            Long departmentId
    );

    List<DepartmentAssignment> findByUserIdAndActiveTrue(
            Long userId
    );

    List<DepartmentAssignment> findByDepartmentIdAndActiveTrue(
            Long departmentId
    );

    Optional<DepartmentAssignment>
    findByUserIdAndDepartmentIdAndActiveTrue(
            Long userId,
            Long departmentId
    );
}