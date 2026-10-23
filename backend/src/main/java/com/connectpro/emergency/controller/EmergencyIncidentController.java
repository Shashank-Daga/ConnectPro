package com.connectpro.emergency.controller;

import com.connectpro.emergency.dto.CreateEmergencyIncidentRequest;
import com.connectpro.emergency.dto.EmergencyIncidentResponse;
import com.connectpro.emergency.service.EmergencyIncidentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/emergencies")
public class EmergencyIncidentController {

    private final EmergencyIncidentService emergencyIncidentService;

    public EmergencyIncidentController(
            EmergencyIncidentService emergencyIncidentService
    ) {
        this.emergencyIncidentService = emergencyIncidentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmergencyIncidentResponse create(
            Authentication authentication,
            @Valid @RequestBody CreateEmergencyIncidentRequest request
    ) {
        return emergencyIncidentService.create(
                authentication,
                request
        );
    }

    @GetMapping
    public List<EmergencyIncidentResponse> getAll(
            Authentication authentication
    ) {
        return emergencyIncidentService.getAll(authentication);
    }

    @GetMapping("/{incidentId}")
    public EmergencyIncidentResponse getById(
            Authentication authentication,
            @PathVariable UUID incidentId
    ) {
        return emergencyIncidentService.getById(
                authentication,
                incidentId
        );
    }

    @PostMapping("/{incidentId}/activate")
    public EmergencyIncidentResponse activate(
            Authentication authentication,
            @PathVariable UUID incidentId
    ) {
        return emergencyIncidentService.activate(
                authentication,
                incidentId
        );
    }

    @PostMapping("/{incidentId}/resolve")
    public EmergencyIncidentResponse resolve(
            Authentication authentication,
            @PathVariable UUID incidentId
    ) {
        return emergencyIncidentService.resolve(
                authentication,
                incidentId
        );
    }

    @PostMapping("/{incidentId}/cancel")
    public EmergencyIncidentResponse cancel(
            Authentication authentication,
            @PathVariable UUID incidentId
    ) {
        return emergencyIncidentService.cancel(
                authentication,
                incidentId
        );
    }
}
