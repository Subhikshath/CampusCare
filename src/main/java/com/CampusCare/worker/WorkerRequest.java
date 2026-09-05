package com.CampusCare.worker;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class WorkerRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "College email is required")
    @Email(message = "Invalid college email")
    private String collegeEmail;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must contain at least 8 characters")
    private String password;

    @NotEmpty(message = "At least one department is required")
    private List<Long> departmentIds;
}