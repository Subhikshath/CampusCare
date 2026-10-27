package com.CampusCare.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

        @Bean
        public AccessDeniedHandler accessDeniedHandler() {
                return (request, response, exception) -> {
                        response.setStatus(403);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"You do not have permission to access this resource\"}");
                };
        }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(Customizer.withDefaults())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))

                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(authenticationEntryPoint)
                                .accessDeniedHandler(accessDeniedHandler()))

                .authorizeHttpRequests(auth -> auth

                        // Login / authentication
                        .requestMatchers("/", "/health")
                        .permitAll()

                        .requestMatchers("/auth/login")
                        .permitAll()

                        .requestMatchers("/auth/change-password")
                        .authenticated()

                        // Main Admin only
                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")

                        // Student
                        .requestMatchers("/student/**")
                        .hasRole("STUDENT")

                        // Department Admin
                        .requestMatchers("/department/**")
                        .hasRole("DEPARTMENT_ADMIN")

                        .requestMatchers("/department-admin/**")
                        .hasRole("DEPARTMENT_ADMIN")

                        // Worker
                        .requestMatchers("/worker/**")
                        .hasRole("WORKER")

                        // Everything else requires login
                        .anyRequest()
                        .authenticated()
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration.getAuthenticationManager();
    }
}