package com.connectpro.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "connectpro.security.jwt")
public record JwtProperties(
        long accessTokenExpiration,
        long refreshTokenExpiration,
        String issuer,
        String audience,
        String keyId,
        String publicKey,
        String privateKey
) {
    public JwtProperties {
        if (accessTokenExpiration <= 0) {
            throw new IllegalArgumentException(
                    "JWT access-token-expiration must be greater than zero"
            );
        }

        if (refreshTokenExpiration <= 0) {
            throw new IllegalArgumentException(
                    "JWT refresh-token-expiration must be greater than zero"
            );
        }

        requireText(issuer, "JWT issuer");
        requireText(audience, "JWT audience");
        requireText(keyId, "JWT key-id");
        requireText(publicKey, "JWT public-key");
        requireText(privateKey, "JWT private-key");
    }

    private static void requireText(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(
                    propertyName + " must not be blank"
            );
        }
    }
}
