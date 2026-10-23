package com.connectpro.emergency.validation;

import com.connectpro.common.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocationTimestampValidatorTest {

    @Test
    void nullTimestampShouldBeAllowed() {
        assertDoesNotThrow(() ->
                LocationTimestampValidator.validateNotTooFarInFuture(
                        null,
                        "recordedAt"
                )
        );
    }

    @Test
    void currentTimestampShouldBeAllowed() {
        assertDoesNotThrow(() ->
                LocationTimestampValidator.validateNotTooFarInFuture(
                        OffsetDateTime.now(),
                        "recordedAt"
                )
        );
    }

    @Test
    void timestampFarInFutureShouldBeRejected() {
        OffsetDateTime future =
                OffsetDateTime.now().plusMinutes(10);

        assertThrows(
                InvalidRequestException.class,
                () -> LocationTimestampValidator.validateNotTooFarInFuture(
                        future,
                        "recordedAt"
                )
        );
    }
}
