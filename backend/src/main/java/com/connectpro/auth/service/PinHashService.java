package com.connectpro.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PinHashService {

    private final PasswordEncoder passwordEncoder;

    public PinHashService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String hash(String pin) {
        return passwordEncoder.encode(pin);
    }

    public boolean matches(String pin, String hashedPin) {
        return passwordEncoder.matches(pin, hashedPin);
    }
}
