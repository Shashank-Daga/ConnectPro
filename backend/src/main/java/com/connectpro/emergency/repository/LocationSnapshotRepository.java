package com.connectpro.emergency.repository;

import com.connectpro.emergency.entity.LocationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LocationSnapshotRepository
        extends JpaRepository<LocationSnapshot, UUID> {

    Optional<LocationSnapshot> findByIncidentId(UUID incidentId);

    Optional<LocationSnapshot> findByIdAndIncidentUserId(
            UUID id,
            UUID userId
    );

    Optional<LocationSnapshot> findByIncidentIdAndIncidentUserId(
            UUID incidentId,
            UUID userId
    );

    boolean existsByIncidentId(UUID incidentId);
}
