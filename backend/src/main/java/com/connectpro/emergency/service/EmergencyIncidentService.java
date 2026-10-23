package com.connectpro.emergency.service;

import com.connectpro.common.exception.ResourceNotFoundException;
import com.connectpro.common.security.AuthenticatedUser;
import com.connectpro.emergency.dto.CreateEmergencyIncidentRequest;
import com.connectpro.emergency.dto.EmergencyIncidentResponse;
import com.connectpro.emergency.entity.EmergencyIncident;
import com.connectpro.emergency.repository.EmergencyIncidentRepository;
import com.connectpro.user.entity.User;
import com.connectpro.user.repository.UserRepository;
import com.connectpro.emergency.dto.EmergencyEventType;
import com.connectpro.emergency.realtime.EmergencyEventPublisher;
import com.connectpro.emergency.service.TrustedContactNotificationService;
import com.connectpro.emergency.model.EmergencyIncidentStatus;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.Map;

@Service
public class EmergencyIncidentService {

    private final EmergencyIncidentRepository emergencyIncidentRepository;
    private final UserRepository userRepository;
    private final EmergencyEventPublisher emergencyEventPublisher;
    private final TrustedContactNotificationService trustedContactNotificationService;

    public EmergencyIncidentService(
            EmergencyIncidentRepository emergencyIncidentRepository,
            UserRepository userRepository,
            EmergencyEventPublisher emergencyEventPublisher,
            TrustedContactNotificationService trustedContactNotificationService
    ) {
        this.emergencyIncidentRepository = emergencyIncidentRepository;
        this.userRepository = userRepository;
        this.emergencyEventPublisher = emergencyEventPublisher;
        this.trustedContactNotificationService = trustedContactNotificationService;
    }

    @Transactional
    public EmergencyIncidentResponse create(
            Authentication authentication,
            CreateEmergencyIncidentRequest request
    ) {
        UUID userId = AuthenticatedUser.getUserId(authentication);

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        EmergencyIncident incident = new EmergencyIncident(
                user,
                request.category()
        );

        EmergencyIncident savedIncident =
                emergencyIncidentRepository.save(incident);

        emergencyEventPublisher.publishAfterCommit(
                userId,
                savedIncident.getId(),
                EmergencyEventType.INCIDENT_CREATED,
                Map.of(
                        "category", savedIncident.getCategory().name(),
                        "status", savedIncident.getStatus().name()
                )
        );

        return toResponse(savedIncident);
    }

    @Transactional(readOnly = true)
    public List<EmergencyIncidentResponse> getAll(
            Authentication authentication
    ) {
        UUID userId = AuthenticatedUser.getUserId(authentication);

        return emergencyIncidentRepository
                .findAllByUserIdOrderByTriggeredAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmergencyIncidentResponse getById(
            Authentication authentication,
            UUID incidentId
    ) {
        UUID userId = AuthenticatedUser.getUserId(authentication);

        EmergencyIncident incident = findOwnedIncident(
                userId,
                incidentId
        );

        return toResponse(incident);
    }

    @Transactional
    public EmergencyIncidentResponse activate(
            Authentication authentication,
            UUID incidentId
    ) {
        UUID userId = AuthenticatedUser.getUserId(authentication);

        EmergencyIncident incident = findOwnedIncident(
                userId,
                incidentId
        );

        incident.activate();

        trustedContactNotificationService.queueAlertsForActivation(incident);

        emergencyEventPublisher.publishAfterCommit(
                userId,
                incident.getId(),
                EmergencyEventType.INCIDENT_ACTIVATED,
                Map.of("status", incident.getStatus().name())
        );

        return toResponse(incident);
    }

    @Transactional
    public EmergencyIncidentResponse resolve(
            Authentication authentication,
            UUID incidentId
    ) {
        UUID userId = AuthenticatedUser.getUserId(authentication);

        EmergencyIncident incident = findOwnedIncident(
                userId,
                incidentId
        );

        incident.resolve();

        emergencyEventPublisher.publishAfterCommit(
                userId,
                incident.getId(),
                EmergencyEventType.INCIDENT_RESOLVED,
                Map.of("status", incident.getStatus().name())
        );

        return toResponse(incident);
    }

    @Transactional
    public EmergencyIncidentResponse cancel(
            Authentication authentication,
            UUID incidentId
    ) {
        UUID userId = AuthenticatedUser.getUserId(authentication);

        EmergencyIncident incident = findOwnedIncident(
                userId,
                incidentId
        );

        incident.cancel();

        emergencyEventPublisher.publishAfterCommit(
                userId,
                incident.getId(),
                EmergencyEventType.INCIDENT_CANCELLED,
                Map.of("status", incident.getStatus().name())
        );

        return toResponse(incident);
    }

    private EmergencyIncident findOwnedIncident(
            UUID userId,
            UUID incidentId
    ) {
        return emergencyIncidentRepository
                .findByIdAndUserId(incidentId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Emergency incident not found"
                        ));
    }

    private EmergencyIncidentResponse toResponse(
            EmergencyIncident incident
    ) {
        return new EmergencyIncidentResponse(
                incident.getId(),
                incident.getCategory(),
                incident.getStatus(),
                incident.getTriggeredAt(),
                incident.getResolvedAt(),
                incident.getCreatedAt(),
                incident.getUpdatedAt()
        );
    }
}
