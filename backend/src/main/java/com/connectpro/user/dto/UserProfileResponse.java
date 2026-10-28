package com.connectpro.user.dto;

import com.connectpro.user.model.UserStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String phoneNumber,
        String email,
        UserStatus status,
        OffsetDateTime phoneVerifiedAt,
        OffsetDateTime emailVerifiedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
