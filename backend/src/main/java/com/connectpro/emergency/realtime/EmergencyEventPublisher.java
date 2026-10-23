
package com.connectpro.emergency.realtime;

import com.connectpro.emergency.dto.EmergencyEventType;
import com.connectpro.emergency.dto.EmergencyRealtimeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class EmergencyEventPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(EmergencyEventPublisher.class);

    private final EmergencyWebSocketHandler webSocketHandler;

    public EmergencyEventPublisher(
            EmergencyWebSocketHandler webSocketHandler
    ) {
        this.webSocketHandler = webSocketHandler;
    }

    public void publishAfterCommit(
            UUID userId,
            UUID incidentId,
            EmergencyEventType eventType,
            Map<String, Object> data
    ) {
        Runnable publishAction = () -> {
            try {
                EmergencyRealtimeEvent event =
                        new EmergencyRealtimeEvent(
                                UUID.randomUUID(),
                                eventType,
                                incidentId,
                                OffsetDateTime.now(),
                                data
                        );

                webSocketHandler.sendToUser(userId, event);

            } catch (RuntimeException exception) {
                log.error(
                        "Failed to publish emergency event. "
                                + "eventType={}, incidentId={}, userId={}",
                        eventType,
                        incidentId,
                        userId,
                        exception
                );
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager
                .isActualTransactionActive()) {

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            publishAction.run();
                        }
                    }
            );

        } else {
            publishAction.run();
        }
    }
}
