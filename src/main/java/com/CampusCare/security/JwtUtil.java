package com.CampusCare.security;

import com.CampusCare.user.User;
import com.CampusCare.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

        @Value("${jwt.secret}")
        private String secret;

        @Value("${jwt.expiration}")
        private long expiration;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        secretKey = Keys.hmacShaKeyFor(
                                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    // ============================
    // Generate JWT Token
    // ============================
    public String generateToken(User user) {

        return Jwts.builder()

                .subject(user.getCollegeEmail())

                .claim("userId", user.getId())

                .claim("role", user.getRole().name())

                .issuedAt(new Date())

                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                                + expiration
                        )
                )

                .signWith(secretKey)

                .compact();
    }

    // ============================
    // Extract Email
    // ============================
    public String extractUsername(String token) {

        return extractClaims(token).getSubject();

    }

    // ============================
    // Extract User Id
    // ============================
    public Long extractUserId(String token) {

        return extractClaims(token)
                .get("userId", Long.class);

    }

    // ============================
    // Extract Role
    // ============================
    public String extractRole(String token) {

        return extractClaims(token)
                .get("role", String.class);

    }

    // ============================
    // Validate Token
    // ============================
    public boolean isTokenValid(String token,
                                UserDetails userDetails) {

        String username = extractUsername(token);

        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token);

    }

    // ============================
    // Check Expiration
    // ============================
    private boolean isTokenExpired(String token) {

        return extractClaims(token)
                .getExpiration()
                .before(new Date());

    }

    // ============================
    // Extract Claims
    // ============================
    private Claims extractClaims(String token) {

        return Jwts.parser()

                .verifyWith(secretKey)

                .build()

                .parseSignedClaims(token)

                .getPayload();

    }

}