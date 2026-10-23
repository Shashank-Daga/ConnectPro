package com.connectpro.emergency.controller;

import com.connectpro.emergency.dto.TrustedContactNotificationResponse;
import com.connectpro.emergency.service.TrustedContactNotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/emergencies/{incidentId}/notifications")
public class TrustedContactNotificationController {

    private final TrustedContactNotificationService notificationService;

    public TrustedContactNotificationController(
            TrustedContactNotificationService notificationService
    ) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<TrustedContactNotificationResponse> getForIncident(
            Authentication authentication,
            @PathVariable UUID incidentId
    ) {
        return notificationService.getForIncident(
                authentication,
                incidentId
        );
    }
}
