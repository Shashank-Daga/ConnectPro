package com.connectpro.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenHashService {

    private final PasswordEncoder passwordEncoder;

    public RefreshTokenHashService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String hash(String refreshToken) {
        return passwordEncoder.encode(refreshToken);
    }

    public boolean matches(String refreshToken, String hashedToken) {
        return passwordEncoder.matches(refreshToken, hashedToken);
    }
}
