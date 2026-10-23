package com.connectpro.emergency.dto;

import com.connectpro.emergency.model.EmergencyCategory;
import com.connectpro.emergency.model.EmergencyIncidentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EmergencyIncidentResponse(
        UUID id,
        EmergencyCategory category,
        EmergencyIncidentStatus status,
        OffsetDateTime triggeredAt,
        OffsetDateTime resolvedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
