package com.connectpro.emergency.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record EmergencyRealtimeEvent(
        UUID eventId,
        EmergencyEventType eventType,
        UUID incidentId,
        OffsetDateTime occurredAt,
        Map<String, Object> data
) {
    public EmergencyRealtimeEvent {
        data = data == null ? Map.of() : Map.copyOf(data);
    }
}
