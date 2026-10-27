package com.CampusCare.department;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class DepartmentResponse {

    Long id;
    String name;
    String code;
    boolean enabled;
    LocalDateTime createdAt;

    public static DepartmentResponse from(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .code(department.getCode())
                .enabled(department.isEnabled())
                .createdAt(department.getCreatedAt())
                .build();
    }
}