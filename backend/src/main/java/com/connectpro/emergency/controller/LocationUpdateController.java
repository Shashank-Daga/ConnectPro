package com.connectpro.emergency.controller;

import com.connectpro.emergency.dto.CreateLocationUpdateRequest;
import com.connectpro.emergency.dto.LocationUpdateResponse;
import com.connectpro.emergency.service.LocationUpdateService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/emergencies/{incidentId}/location")
public class LocationUpdateController {

    private final LocationUpdateService locationUpdateService;

    public LocationUpdateController(
            LocationUpdateService locationUpdateService
    ) {
        this.locationUpdateService = locationUpdateService;
    }

    @PostMapping("/updates")
    public LocationUpdateResponse create(
            Authentication authentication,
            @PathVariable UUID incidentId,
            @Valid @RequestBody CreateLocationUpdateRequest request
    ) {
        return locationUpdateService.create(
                authentication,
                incidentId,
                request
        );
    }

    @GetMapping("/updates")
    public List<LocationUpdateResponse> getHistory(
            Authentication authentication,
            @PathVariable UUID incidentId
    ) {
        return locationUpdateService.getHistory(
                authentication,
                incidentId
        );
    }

    @GetMapping("/current")
    public Object getCurrent(
            Authentication authentication,
            @PathVariable UUID incidentId
    ) {
        return locationUpdateService.getCurrent(
                authentication,
                incidentId
        );
    }
}
