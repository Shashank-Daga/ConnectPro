package com.connectpro.emergency.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LocationSnapshotResponse(
        UUID id,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal accuracyMeters,
        OffsetDateTime capturedAt
) {
}
