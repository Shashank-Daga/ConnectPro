package com.connectpro.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyPhoneRequest(

        @NotBlank(message = "Phone number is required")
        @Pattern(
                regexp = "^[0-9]{10}$",
                message = "Phone number must contain exactly 10 digits"
        )
        String phoneNumber,

        @NotBlank(message = "Verification code is required")
        @Pattern(
                regexp = "^\\d{6}$",
                message = "Verification code must be 6 digits"
        )
        String verificationCode
) {
}
