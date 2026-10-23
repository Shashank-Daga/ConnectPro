package com.connectpro.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RequestPinResetRequest(

        @NotBlank(message = "Recovery identifier is required")
        @Size(max = 254, message = "Recovery identifier is too long")
        String identifier
) {
}
