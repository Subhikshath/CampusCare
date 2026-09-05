package com.CampusCare.auth;

import com.CampusCare.security.JwtUtil;
import com.CampusCare.user.User;
import com.CampusCare.user.UserRepository;
import com.CampusCare.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtUtil jwtUtil;

    // Student eligibility validation
    public void validateStudentEligibility(User student) {
        if (student.getRole() != UserRole.STUDENT || student.getRollNo() == null
                || student.getRollNo().isBlank()) {
            throw new IllegalArgumentException("Only students with a roll number can submit complaints");
        }
    }
    // ===========================
    // Login
    // ===========================

    public LoginResponse login(LoginRequest request) {

        authenticationManager.authenticate(

                new UsernamePasswordAuthenticationToken(

                        request.getCollegeEmail(),

                        request.getPassword()

                )

        );

        User user = userRepository.findByCollegeEmail(
                        request.getCollegeEmail())

                .orElseThrow(() ->
                        new RuntimeException("User Not Found"));

        String token = jwtUtil.generateToken(user);

        return LoginResponse.builder()

                .token(token)

                .userId(user.getId())

                .name(user.getName())

                .collegeEmail(user.getCollegeEmail())

                .role(user.getRole().name())

                .build();

    }

    @Transactional
    public void changePassword(String collegeEmail,
                               ChangePasswordRequest request) {
        User user = userRepository.findByCollegeEmail(collegeEmail)
                .orElseThrow(() -> new RuntimeException("User Not Found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

}