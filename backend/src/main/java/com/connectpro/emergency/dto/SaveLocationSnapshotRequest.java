package com.connectpro.emergency.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SaveLocationSnapshotRequest(

        @NotNull
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        BigDecimal latitude,

        @NotNull
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        BigDecimal longitude,

        @PositiveOrZero
        BigDecimal accuracyMeters,

        OffsetDateTime capturedAt
) {
}
