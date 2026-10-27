package com.CampusCare.config;

import com.CampusCare.user.User;
import com.CampusCare.user.UserRepository;
import com.CampusCare.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

@Configuration
@Profile("prod")
@RequiredArgsConstructor
public class ProductionAdminBootstrap {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${INITIAL_ADMIN_NAME:}")
    private String adminName;

    @Value("${INITIAL_ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${INITIAL_ADMIN_PASSWORD:}")
    private String adminPassword;

    @Bean
    public CommandLineRunner createInitialAdmin() {
        return args -> {
            if (!userRepository.findByRole(UserRole.ADMIN).isEmpty()) {
                return;
            }
            if (!StringUtils.hasText(adminName)
                    || !StringUtils.hasText(adminEmail)
                    || !StringUtils.hasText(adminPassword)) {
                throw new IllegalStateException(
                        "No production administrator exists. Configure INITIAL_ADMIN_NAME, "
                                + "INITIAL_ADMIN_EMAIL, and INITIAL_ADMIN_PASSWORD to provision one.");
            }
            if (adminPassword.length() < 12) {
                throw new IllegalStateException(
                        "INITIAL_ADMIN_PASSWORD must be at least 12 characters long.");
            }

            userRepository.save(User.builder()
                    .name(adminName.trim())
                    .collegeEmail(adminEmail.trim())
                    .password(passwordEncoder.encode(adminPassword))
                    .role(UserRole.ADMIN)
                    .enabled(true)
                    .build());
        };
    }
}
