package com.connectpro.emergency.dto;

import com.connectpro.emergency.model.TrustedContactNotificationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TrustedContactNotificationResponse(
        UUID id,
        String contactName,
        String maskedPhoneNumber,
        TrustedContactNotificationStatus status,
        int deliveryAttempts,
        OffsetDateTime sentAt,
        OffsetDateTime createdAt
) {
}
