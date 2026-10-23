package com.connectpro.emergency.service;

import com.connectpro.common.exception.ResourceNotFoundException;
import com.connectpro.common.security.AuthenticatedUser;
import com.connectpro.emergency.dto.LocationSnapshotResponse;
import com.connectpro.emergency.dto.SaveLocationSnapshotRequest;
import com.connectpro.emergency.entity.EmergencyIncident;
import com.connectpro.emergency.entity.LocationSnapshot;
import com.connectpro.emergency.repository.EmergencyIncidentRepository;
import com.connectpro.emergency.repository.LocationSnapshotRepository;
import com.connectpro.common.exception.ResourceConflictException;
import com.connectpro.common.exception.InvalidStateTransitionException;
import com.connectpro.emergency.model.EmergencyIncidentStatus;

import com.connectpro.emergency.validation.LocationTimestampValidator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class LocationSnapshotService {

    private final LocationSnapshotRepository locationSnapshotRepository;
    private final EmergencyIncidentRepository emergencyIncidentRepository;

    public LocationSnapshotService(
            LocationSnapshotRepository locationSnapshotRepository,
            EmergencyIncidentRepository emergencyIncidentRepository
    ) {
        this.locationSnapshotRepository = locationSnapshotRepository;
        this.emergencyIncidentRepository = emergencyIncidentRepository;
    }

    @Transactional
    public LocationSnapshotResponse save(
            Authentication authentication,
            UUID incidentId,
            SaveLocationSnapshotRequest request
    ) {
        UUID userId = AuthenticatedUser.getUserId(authentication);

        EmergencyIncident incident =
                emergencyIncidentRepository
                        .findByIdAndUserId(incidentId, userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Emergency incident not found"
                                ));

        if (incident.getStatus() != EmergencyIncidentStatus.CREATED
                && incident.getStatus() != EmergencyIncidentStatus.ACTIVE) {
            throw new InvalidStateTransitionException(
                    "Location cannot be added to an inactive incident"
            );
        }

        if (locationSnapshotRepository.existsByIncidentId(incidentId)) {
            throw new ResourceConflictException(
                    "Location snapshot already exists for this incident"
            );
        }

        LocationTimestampValidator.validateNotTooFarInFuture(
                request.capturedAt(),
                "capturedAt"
        );

        LocationSnapshot snapshot = new LocationSnapshot(
                incident,
                request.latitude(),
                request.longitude(),
                request.accuracyMeters(),
                request.capturedAt() != null
                        ? request.capturedAt()
                        : OffsetDateTime.now()
        );

        return toResponse(
                locationSnapshotRepository.save(snapshot)
        );
    }

    @Transactional(readOnly = true)
    public LocationSnapshotResponse get(
            Authentication authentication,
            UUID incidentId
    ) {
        UUID userId = AuthenticatedUser.getUserId(authentication);

        LocationSnapshot snapshot =
                locationSnapshotRepository
                        .findByIncidentIdAndIncidentUserId(
                                incidentId,
                                userId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Location snapshot not found"
                                ));

        return toResponse(snapshot);
    }

    private LocationSnapshotResponse toResponse(
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
