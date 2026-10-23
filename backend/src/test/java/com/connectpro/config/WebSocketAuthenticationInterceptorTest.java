
package com.connectpro.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.socket.WebSocketHandler;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketAuthenticationInterceptorTest {

    private JwtDecoder jwtDecoder;
    private WebSocketAuthenticationInterceptor interceptor;
    private ServerHttpRequest request;
    private ServerHttpResponse response;
    private WebSocketHandler webSocketHandler;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        jwtDecoder = mock(JwtDecoder.class);
        interceptor = new WebSocketAuthenticationInterceptor(jwtDecoder);

        request = mock(ServerHttpRequest.class);
        response = mock(ServerHttpResponse.class);
        webSocketHandler = mock(WebSocketHandler.class);
        attributes = new HashMap<>();
    }

    @Test
    void validJwtShouldAuthenticateAndStoreUserId() {
        UUID userId = UUID.randomUUID();
        String token = "valid-token";

        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject(userId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        when(request.getHeaders()).thenReturn(headers);
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        boolean accepted = interceptor.beforeHandshake(
                request, response, webSocketHandler, attributes
        );

        assertTrue(accepted);
        assertEquals(userId, attributes.get("userId"));
        verify(jwtDecoder).decode(token);
    }

    @Test
    void missingAuthorizationHeaderShouldBeRejected() {
        when(request.getHeaders()).thenReturn(new HttpHeaders());

        boolean accepted = interceptor.beforeHandshake(
                request, response, webSocketHandler, attributes
        );

        assertFalse(accepted);
        assertTrue(attributes.isEmpty());
        verify(jwtDecoder, never()).decode(
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void blankBearerTokenShouldBeRejected() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer   ");

        when(request.getHeaders()).thenReturn(headers);

        boolean accepted = interceptor.beforeHandshake(
                request, response, webSocketHandler, attributes
        );

        assertFalse(accepted);
        assertTrue(attributes.isEmpty());
        verify(jwtDecoder, never()).decode(
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void invalidJwtShouldBeRejected() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalid-token");

        when(request.getHeaders()).thenReturn(headers);
        when(jwtDecoder.decode("invalid-token"))
                .thenThrow(new JwtException("Invalid token"));

        boolean accepted = interceptor.beforeHandshake(
                request, response, webSocketHandler, attributes
        );

        assertFalse(accepted);
        assertTrue(attributes.isEmpty());
    }

    @Test
    void nonUuidSubjectShouldBeRejected() {
        String token = "valid-signature-but-invalid-subject";

        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject("not-a-uuid")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        when(request.getHeaders()).thenReturn(headers);
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        boolean accepted = interceptor.beforeHandshake(
                request, response, webSocketHandler, attributes
        );

        assertFalse(accepted);
        assertTrue(attributes.isEmpty());
    }


    @Test
    void blankSubjectShouldBeRejected() {
        String token = "token-with-blank-subject";

        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject(" ")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        when(request.getHeaders()).thenReturn(headers);
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        boolean accepted = interceptor.beforeHandshake(
                request, response, webSocketHandler, attributes
        );

        assertFalse(accepted);
        assertTrue(attributes.isEmpty());
    }
}
