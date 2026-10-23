package com.connectpro.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateEmergencyContactRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        @NotBlank
        @Size(max = 30)
        String phoneNumber,

        @NotBlank
        @Size(max = 50)
        String relationship

) {
}
