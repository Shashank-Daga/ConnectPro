package com.connectpro.user.dto;

import com.connectpro.user.model.EmergencyContactStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EmergencyContactResponse(
        UUID id,
        String name,
        String phoneNumber,
        String relationship,
        EmergencyContactStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
