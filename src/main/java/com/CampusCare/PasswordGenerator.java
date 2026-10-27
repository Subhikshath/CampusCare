package com.CampusCare;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {

    public static void main(String[] args) {
        if (args.length != 1 || args[0].isBlank()) {
            throw new IllegalArgumentException("Provide the plaintext password as the only argument");
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println(encoder.encode(args[0]));
    }
}