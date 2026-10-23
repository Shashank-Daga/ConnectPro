package com.connectpro.emergency.controller;

import com.connectpro.emergency.dto.LocationSnapshotResponse;
import com.connectpro.emergency.dto.SaveLocationSnapshotRequest;
import com.connectpro.emergency.service.LocationSnapshotService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/emergencies/{incidentId}/location")
public class LocationSnapshotController {

    private final LocationSnapshotService locationSnapshotService;

    public LocationSnapshotController(
            LocationSnapshotService locationSnapshotService
    ) {
        this.locationSnapshotService = locationSnapshotService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocationSnapshotResponse save(
            Authentication authentication,
            @PathVariable UUID incidentId,
            @Valid @RequestBody SaveLocationSnapshotRequest request
    ) {
        return locationSnapshotService.save(
                authentication,
                incidentId,
                request
        );
    }

    @GetMapping
    public LocationSnapshotResponse get(
            Authentication authentication,
            @PathVariable UUID incidentId
    ) {
        return locationSnapshotService.get(
                authentication,
                incidentId
        );
    }
}
