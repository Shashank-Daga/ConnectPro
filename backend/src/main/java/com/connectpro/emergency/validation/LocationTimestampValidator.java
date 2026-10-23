package com.connectpro.emergency.validation;

import com.connectpro.common.exception.InvalidRequestException;

import java.time.OffsetDateTime;

public final class LocationTimestampValidator {

    private static final long ALLOWED_FUTURE_SKEW_MINUTES = 2;

    private LocationTimestampValidator() {
    }

    public static void validateNotTooFarInFuture(
            OffsetDateTime timestamp,
            String fieldName
    ) {
        if (timestamp == null) {
            return;
        }

        OffsetDateTime latestAllowed =
                OffsetDateTime.now().plusMinutes(
                        ALLOWED_FUTURE_SKEW_MINUTES
                );

        if (timestamp.isAfter(latestAllowed)) {
            throw new InvalidRequestException(
                    fieldName + " must not be more than 2 minutes in the future"
            );
        }
    }
}
