package com.CampusCare.department;

import com.CampusCare.user.User;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class DepartmentAdminResponse {

    Long userId;
    String name;
    String collegeEmail;
    String role;
    List<Long> departmentIds;

    public static DepartmentAdminResponse from(User user) {
        return DepartmentAdminResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .collegeEmail(user.getCollegeEmail())
                .role(user.getRole().name())
                .departmentIds(user.getDepartments().stream()
                        .map(Department::getId)
                        .toList())
                .build();
    }
}