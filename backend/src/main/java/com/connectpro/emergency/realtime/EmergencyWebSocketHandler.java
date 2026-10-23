package com.connectpro.emergency.realtime;

import com.connectpro.emergency.dto.EmergencyRealtimeEvent;

import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EmergencyWebSocketHandler extends TextWebSocketHandler {

    private static final String USER_ID_ATTRIBUTE = "userId";

    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<UUID, Set<WebSocketSession>> sessionsByUser =
            new ConcurrentHashMap<>();

    public EmergencyWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Object value = session.getAttributes().get(USER_ID_ATTRIBUTE);

        if (!(value instanceof UUID userId)) {
            closeQuietly(session, CloseStatus.POLICY_VIOLATION);
            return;
        }

        sessionsByUser
                .computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message
    ) throws IOException {
        // This endpoint currently delivers server-originated events only.
        // Client commands will be introduced separately with explicit validation.
        session.close(CloseStatus.NOT_ACCEPTABLE);
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {
        Object value = session.getAttributes().get(USER_ID_ATTRIBUTE);

        if (!(value instanceof UUID userId)) {
            return;
        }

        Set<WebSocketSession> sessions = sessionsByUser.get(userId);

        if (sessions != null) {
            sessions.remove(session);

            if (sessions.isEmpty()) {
                sessionsByUser.remove(userId, sessions);
            }
        }
    }

    public void sendToUser(UUID userId, EmergencyRealtimeEvent event) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);

        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        final String payload = objectMapper.writeValueAsString(event);

        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                removeSession(userId, session);
                continue;
            }

            try {
                synchronized (session) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(payload));
                    }
                }
            } catch (IOException | RuntimeException exception) {
                closeQuietly(session, CloseStatus.SERVER_ERROR);
                removeSession(userId, session);
            }
        }
    }

    private void removeSession(UUID userId, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);

        if (sessions != null) {
            sessions.remove(session);

            if (sessions.isEmpty()) {
                sessionsByUser.remove(userId, sessions);
            }
        }
    }

    private void closeQuietly(
            WebSocketSession session,
            CloseStatus status
    ) {
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (IOException ignored) {
            // The connection is already unavailable.
        }
    }
}
