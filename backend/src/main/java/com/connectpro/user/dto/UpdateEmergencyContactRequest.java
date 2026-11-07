package com.connectpro.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateEmergencyContactRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        @NotBlank
        @Pattern(
                regexp = "^[0-9]{10}$",
                message = "Phone number must contain exactly 10 digits"
        )
        String phoneNumber,

        @NotBlank
        @Size(max = 50)
        String relationship

) {
}
