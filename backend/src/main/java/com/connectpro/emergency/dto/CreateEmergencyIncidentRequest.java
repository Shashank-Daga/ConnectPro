package com.connectpro.emergency.dto;

import com.connectpro.emergency.model.EmergencyCategory;
import jakarta.validation.constraints.NotNull;

public record CreateEmergencyIncidentRequest(

        @NotNull
        EmergencyCategory category
) {
}
