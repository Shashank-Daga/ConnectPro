package com.connectpro.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPinRequest(

        @NotBlank(message = "Recovery identifier is required")
        @Size(max = 254, message = "Recovery identifier is too long")
        String identifier,

        @NotBlank(message = "Verification code is required")
        @Pattern(
                regexp = "^\\d{6}$",
                message = "Verification code must be 6 digits"
        )
        String verificationCode,

        @NotBlank(message = "New PIN is required")
        @Size(
                min = 6,
                max = 64,
                message = "PIN must be between 6 and 64 characters"
        )
        @Pattern(
                regexp = "^[A-Za-z0-9]+$",
                message = "PIN must contain only letters and numbers"
        )
        String newPin
) {
}
