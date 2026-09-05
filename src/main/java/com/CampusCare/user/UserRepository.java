package com.CampusCare.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByCollegeEmail(String collegeEmail);

    boolean existsByCollegeEmail(String collegeEmail);




    boolean existsByRollNo(String rollNo);

    List<User> findByRole(UserRole role);
}