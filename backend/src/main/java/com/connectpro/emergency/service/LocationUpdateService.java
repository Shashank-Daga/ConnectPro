package com.connectpro.emergency.service;

import com.connectpro.common.exception.InvalidStateTransitionException;
import com.connectpro.common.exception.ResourceNotFoundException;
import com.connectpro.common.security.AuthenticatedUser;
import com.connectpro.emergency.dto.CreateLocationUpdateRequest;
import com.connectpro.emergency.dto.LocationSnapshotResponse;
import com.connectpro.emergency.dto.LocationUpdateResponse;
import com.connectpro.emergency.entity.EmergencyIncident;
import com.connectpro.emergency.entity.LocationSnapshot;
import com.connectpro.emergency.entity.LocationUpdate;
import com.connectpro.emergency.model.EmergencyIncidentStatus;
import com.connectpro.emergency.repository.EmergencyIncidentRepository;
import com.connectpro.emergency.repository.LocationSnapshotRepository;
import com.connectpro.emergency.repository.LocationUpdateRepository;
import com.connectpro.auth.service.RateLimitPolicy;
import com.connectpro.auth.service.RateLimitService;
import com.connectpro.emergency.dto.EmergencyEventType;
import com.connectpro.emergency.realtime.EmergencyEventPublisher;
import com.connectpro.emergency.validation.LocationTimestampValidator;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

@Service
public class LocationUpdateService {

    private final LocationUpdateRepository locationUpdateRepository;
    private final LocationSnapshotRepository locationSnapshotRepository;
    private final EmergencyIncidentRepository emergencyIncidentRepository;
    private final RateLimitService rateLimitService;
    private final EmergencyEventPublisher emergencyEventPublisher;

    public LocationUpdateService(
            LocationUpdateRepository locationUpdateRepository,
            LocationSnapshotRepository locationSnapshotRepository,
            EmergencyIncidentRepository emergencyIncidentRepository,
            RateLimitService rateLimitService,
            EmergencyEventPublisher emergencyEventPublisher
    ) {
        this.locationUpdateRepository = locationUpdateRepository;
        this.locationSnapshotRepository = locationSnapshotRepository;
        this.emergencyIncidentRepository = emergencyIncidentRepository;
        this.rateLimitService = rateLimitService;
        this.emergencyEventPublisher = emergencyEventPublisher;
    }

    @Transactional
    public LocationUpdateResponse create(
            Authentication authentication,
            UUID incidentId,
            CreateLocationUpdateRequest request
    ) {
        UUID userId = AuthenticatedUser.getUserId(authentication);

        EmergencyIncident incident =
                emergencyIncidentRepository
                        .findByIdAndUserId(incidentId, userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Emergency incident not found"
                                ));

        if (incident.getStatus() != EmergencyIncidentStatus.ACTIVE) {
            throw new InvalidStateTransitionException(
                    "Live location updates can only be added to an active incident"
            );
        }

        LocationTimestampValidator.validateNotTooFarInFuture(
                request.recordedAt(),
                "recordedAt"
        );

        rateLimitService.checkAndRecord(
                "location-update:user:" + userId,
                RateLimitPolicy.LOCATION_UPDATE_MAX_ATTEMPTS,
                RateLimitPolicy.LOCATION_UPDATE_WINDOW
        );

        OffsetDateTime recordedAt =
                request.recordedAt() != null
                        ? request.recordedAt()
                        : OffsetDateTime.now();

        LocationUpdate locationUpdate = new LocationUpdate(
                incident,
                request.latitude(),
                request.longitude(),
                request.accuracyMeters(),
                recordedAt
        );

        LocationUpdate savedUpdate =
                locationUpdateRepository.save(locationUpdate);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("latitude", savedUpdate.getLatitude());
        eventData.put("longitude", savedUpdate.getLongitude());

        if (savedUpdate.getAccuracyMeters() != null) {
            eventData.put("accuracyMeters", savedUpdate.getAccuracyMeters());
        }

        eventData.put("recordedAt", savedUpdate.getRecordedAt());

        emergencyEventPublisher.publishAfterCommit(
                userId,
                incident.getId(),
                EmergencyEventType.LOCATION_UPDATED,
                eventData
        );

        return toResponse(savedUpdate);
    }

    @Transactional(readOnly = true)
    public List<LocationUpdateResponse> getHistory(
            Authentication authentication,
            UUID incidentId
    ) {
        UUID userId = AuthenticatedUser.getUserId(authentication);

        verifyOwnedIncident(userId, incidentId);

        return locationUpdateRepository
                .findAllByIncidentIdAndIncidentUserIdOrderByRecordedAtAscCreatedAtAsc(
                        incidentId,
                        userId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Object getCurrent(
            Authentication authentication,
            UUID incidentId
    ) {
        UUID userId = AuthenticatedUser.getUserId(authentication);

        verifyOwnedIncident(userId, incidentId);

        LocationUpdate currentUpdate =
                locationUpdateRepository
                        .findFirstByIncidentIdAndIncidentUserIdOrderByRecordedAtDescCreatedAtDesc(
                                incidentId,
                                userId
                        )
                        .orElse(null);

        if (currentUpdate != null) {
            return toResponse(currentUpdate);
        }

        LocationSnapshot initialSnapshot =
                locationSnapshotRepository
                        .findByIncidentIdAndIncidentUserId(
                                incidentId,
                                userId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Current location not found"
                                ));

        return toSnapshotResponse(initialSnapshot);
    }

    private EmergencyIncident verifyOwnedIncident(
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

    private LocationUpdateResponse toResponse(
            LocationUpdate locationUpdate
    ) {
        return new LocationUpdateResponse(
                locationUpdate.getId(),
                locationUpdate.getLatitude(),
                locationUpdate.getLongitude(),
                locationUpdate.getAccuracyMeters(),
                locationUpdate.getRecordedAt(),
                locationUpdate.getCreatedAt()
        );
    }

    private LocationSnapshotResponse toSnapshotResponse(
            LocationSnapshot snapshot
    ) {
        return new LocationSnapshotResponse(
                snapshot.getId(),
                snapshot.getLatitude(),
                snapshot.getLongitude(),
                snapshot.getAccuracyMeters(),
                snapshot.getCapturedAt()
        );
    }
}
