package com.connectpro.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "Phone number is required")
        @Pattern(
                regexp = "^\\+[1-9]\\d{7,14}$",
                message = "Phone number must be in international format"
        )
        String phoneNumber,

        @NotBlank(message = "PIN is required")
        @Size(
                min = 6,
                max = 64,
                message = "PIN must be between 6 and 64 characters"
        )
        @Pattern(
                regexp = "^[A-Za-z0-9]+$",
                message = "PIN must contain only letters and numbers"
        )
        String pin
) {
}
