package com.connectpro.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn              // in secs, access token
) {
}
