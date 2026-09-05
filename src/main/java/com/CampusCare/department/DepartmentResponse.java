package com.CampusCare.department;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DepartmentResponse {

    Long id;
    String name;
    String code;
    boolean enabled;

    public static DepartmentResponse from(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .code(department.getCode())
                .enabled(department.isEnabled())
                .build();
    }
}