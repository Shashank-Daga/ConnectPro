package com.connectpro.common.security;

import com.connectpro.auth.exception.AuthenticationException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public final class AuthenticatedUser {

    private AuthenticatedUser() {
        // Utility class
    }

    public static UUID getUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AuthenticationException("Invalid authentication");
        }

        String subject = jwt.getSubject();

        if (subject == null || subject.isBlank()) {
            throw new AuthenticationException("Invalid authentication");
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new AuthenticationException("Invalid authentication");
        }
    }
}
