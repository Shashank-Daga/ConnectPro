
package com.connectpro.emergency.realtime;

import com.connectpro.emergency.dto.EmergencyEventType;
import com.connectpro.emergency.dto.EmergencyRealtimeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmergencyWebSocketHandlerTest {

    private ObjectMapper objectMapper;
    private EmergencyWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = mock(ObjectMapper.class);
        handler = new EmergencyWebSocketHandler(objectMapper);
    }

    private WebSocketSession createSession(UUID userId) {
        WebSocketSession session = mock(WebSocketSession.class);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", userId);

        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(true);

        return session;
    }

    private EmergencyRealtimeEvent createEvent(UUID incidentId) {
        return new EmergencyRealtimeEvent(
                UUID.randomUUID(),
                EmergencyEventType.INCIDENT_CREATED,
                incidentId,
                OffsetDateTime.now(),
                Map.of("status", "CREATED")
        );
    }

    private void stubSerializedEvent() {
        when(objectMapper.writeValueAsString(
                any(EmergencyRealtimeEvent.class)
        )).thenReturn("{\"eventType\":\"INCIDENT_CREATED\"}");
    }

    @Test
    void connectionShouldRegisterSessionForAuthenticatedUser()
            throws Exception {
        UUID userId = UUID.randomUUID();
        WebSocketSession session = createSession(userId);
        stubSerializedEvent();

        handler.afterConnectionEstablished(session);
        handler.sendToUser(userId, createEvent(UUID.randomUUID()));

        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void missingUserIdShouldCloseSession() throws IOException {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(new HashMap<>());
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test
    void eventShouldBeSentOnlyToTargetUsersSessions()
            throws Exception {
        UUID targetUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        WebSocketSession targetSession = createSession(targetUserId);
        WebSocketSession otherSession = createSession(otherUserId);
        stubSerializedEvent();

        handler.afterConnectionEstablished(targetSession);
        handler.afterConnectionEstablished(otherSession);

        handler.sendToUser(
                targetUserId,
                createEvent(UUID.randomUUID())
        );

        verify(targetSession).sendMessage(any(TextMessage.class));
        verify(otherSession, never())
                .sendMessage(any(TextMessage.class));
    }

    @Test
    void closedSessionShouldNotReceiveEvent() throws Exception {
        UUID userId = UUID.randomUUID();
        WebSocketSession session = createSession(userId);
        stubSerializedEvent();

        handler.afterConnectionEstablished(session);
        when(session.isOpen()).thenReturn(false);

        handler.sendToUser(userId, createEvent(UUID.randomUUID()));

        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void failedSessionShouldNotPreventDeliveryToAnotherSession()
            throws Exception {
        UUID userId = UUID.randomUUID();

        WebSocketSession failingSession = createSession(userId);
        WebSocketSession healthySession = createSession(userId);
        stubSerializedEvent();

        doThrow(new IOException("Simulated network failure"))
                .when(failingSession)
                .sendMessage(any(TextMessage.class));

        handler.afterConnectionEstablished(failingSession);
        handler.afterConnectionEstablished(healthySession);

        handler.sendToUser(userId, createEvent(UUID.randomUUID()));

        verify(failingSession).close(CloseStatus.SERVER_ERROR);
        verify(healthySession).sendMessage(any(TextMessage.class));
    }

    @Test
    void closedConnectionShouldBeRemovedFromRegistry()
            throws Exception {
        UUID userId = UUID.randomUUID();
        WebSocketSession session = createSession(userId);

        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertDoesNotThrow(() ->
                handler.sendToUser(userId, createEvent(UUID.randomUUID()))
        );

        verify(session, never()).sendMessage(any(TextMessage.class));
        verifyNoInteractions(objectMapper);
    }
}
