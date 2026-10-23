package com.connectpro.config;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.UUID;

public class WebSocketAuthenticationInterceptor implements HandshakeInterceptor {

    private final JwtDecoder jwtDecoder;

    public WebSocketAuthenticationInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String authorization = request.getHeaders().getFirst(
                HttpHeaders.AUTHORIZATION
        );

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            return false;
        }

        String token = authorization.substring("Bearer ".length()).trim();

        if (token.isBlank()) {
            return false;
        }

        try {
            Jwt jwt = jwtDecoder.decode(token);

            String subject = jwt.getSubject();

            if (subject == null || subject.isBlank()) {
                return false;
            }

            UUID userId;

            try {
                userId = UUID.fromString(subject);
            } catch (IllegalArgumentException ex) {
                return false;
            }

            attributes.put("userId", userId);

            return true;

        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // No action required.
    }
}
