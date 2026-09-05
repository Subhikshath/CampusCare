package com.CampusCare.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "College Email is required")
    @Email(message = "Invalid Email Format")
    private String collegeEmail;

    @NotBlank(message = "Password is required")
    private String password;

}