package com.connectpro.config;

import com.connectpro.emergency.realtime.EmergencyWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final JwtDecoder jwtDecoder;
    private final EmergencyWebSocketHandler emergencyWebSocketHandler;
    private final String[] allowedOrigins;

    public WebSocketConfig(
            JwtDecoder jwtDecoder,
            EmergencyWebSocketHandler emergencyWebSocketHandler,
            @Value("${connectpro.websocket.allowed-origins}") String originsProperty
    ) {
        this.jwtDecoder = jwtDecoder;
        this.emergencyWebSocketHandler = emergencyWebSocketHandler;

        this.allowedOrigins = Arrays.stream(originsProperty.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toArray(String[]::new);

        if (this.allowedOrigins.length == 0
                || Arrays.asList(this.allowedOrigins).contains("*")) {
            throw new IllegalStateException(
                    "WebSocket allowed origins must be explicit and non-empty"
            );
        }
    }

    @Override
    public void registerWebSocketHandlers(
            WebSocketHandlerRegistry registry
    ) {
        registry.addHandler(
                        emergencyWebSocketHandler,
                        "/ws/emergency-events"
                )
                .addInterceptors(
                        new WebSocketAuthenticationInterceptor(jwtDecoder)
                )
                .setAllowedOrigins(allowedOrigins);
    }
}
