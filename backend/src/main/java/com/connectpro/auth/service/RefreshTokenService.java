package com.connectpro.auth.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private static final int SELECTOR_BYTES = 16;
    private static final int SECRET_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshToken generate() {
        String selector = generateRandomValue(SELECTOR_BYTES);
        String secret = generateRandomValue(SECRET_BYTES);

        return new RefreshToken(selector, secret);
    }

    private String generateRandomValue(int byteLength) {
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    public record RefreshToken(
            String selector,
            String secret
    ) {
        public String value() {
            return selector + "." + secret;
        }
    }
}
