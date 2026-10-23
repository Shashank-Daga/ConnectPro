
package com.connectpro.emergency.realtime;

import com.connectpro.emergency.dto.EmergencyEventType;
import com.connectpro.emergency.dto.EmergencyRealtimeEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EmergencyEventPublisherTest {

    private final EmergencyWebSocketHandler webSocketHandler =
            mock(EmergencyWebSocketHandler.class);

    private final EmergencyEventPublisher publisher =
            new EmergencyEventPublisher(webSocketHandler);

    @AfterEach
    void clearTransactionState() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void shouldPublishImmediatelyWhenNoTransactionIsActive() {
        UUID userId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();

        publisher.publishAfterCommit(
                userId,
                incidentId,
                EmergencyEventType.INCIDENT_CREATED,
                Map.of("status", "CREATED")
        );

        verify(webSocketHandler).sendToUser(
                eq(userId),
                any(EmergencyRealtimeEvent.class)
        );
    }

    @Test
    void shouldWaitUntilAfterCommitWhenTransactionIsActive() {
        UUID userId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        publisher.publishAfterCommit(
                userId,
                incidentId,
                EmergencyEventType.INCIDENT_ACTIVATED,
                Map.of("status", "ACTIVE")
        );

        verifyNoInteractions(webSocketHandler);

        for (TransactionSynchronization synchronization :
                TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }

        verify(webSocketHandler).sendToUser(
                eq(userId),
                any(EmergencyRealtimeEvent.class)
        );
    }

    @Test
    void publicationFailureShouldNotEscapeToCaller() {
        doThrow(new IllegalStateException("Simulated delivery failure"))
                .when(webSocketHandler)
                .sendToUser(any(UUID.class), any(EmergencyRealtimeEvent.class));

        assertDoesNotThrow(() ->
                publisher.publishAfterCommit(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        EmergencyEventType.LOCATION_UPDATED,
                        Map.of("latitude", 18.5, "longitude", 73.8)
                )
        );
    }

    @Test
    void eventShouldContainExpectedIncidentTypeAndData() {
        UUID userId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();

        publisher.publishAfterCommit(
                userId,
                incidentId,
                EmergencyEventType.INCIDENT_RESOLVED,
                Map.of("status", "RESOLVED")
        );

        var eventCaptor =
                org.mockito.ArgumentCaptor.forClass(
                        EmergencyRealtimeEvent.class
                );

        verify(webSocketHandler).sendToUser(
                eq(userId),
                eventCaptor.capture()
        );

        EmergencyRealtimeEvent event = eventCaptor.getValue();

        assertEquals(incidentId, event.incidentId());
        assertEquals(
                EmergencyEventType.INCIDENT_RESOLVED,
                event.eventType()
        );
        assertEquals("RESOLVED", event.data().get("status"));
    }
}
