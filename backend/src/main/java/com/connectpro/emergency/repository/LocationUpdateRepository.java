package com.connectpro.emergency.repository;

import com.connectpro.emergency.entity.LocationUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocationUpdateRepository
        extends JpaRepository<LocationUpdate, UUID> {

    List<LocationUpdate> findAllByIncidentIdOrderByRecordedAtAscCreatedAtAsc(
            UUID incidentId
    );

    List<LocationUpdate> findAllByIncidentIdAndIncidentUserIdOrderByRecordedAtAscCreatedAtAsc(
            UUID incidentId,
            UUID userId
    );

    Optional<LocationUpdate> findFirstByIncidentIdAndIncidentUserIdOrderByRecordedAtDescCreatedAtDesc(
            UUID incidentId,
            UUID userId
    );
}
