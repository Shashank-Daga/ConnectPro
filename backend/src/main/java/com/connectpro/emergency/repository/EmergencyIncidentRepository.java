package com.connectpro.emergency.repository;

import com.connectpro.emergency.entity.EmergencyIncident;
import com.connectpro.emergency.model.EmergencyIncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmergencyIncidentRepository extends JpaRepository<EmergencyIncident, UUID> {

    List<EmergencyIncident> findAllByUserIdOrderByTriggeredAtDesc(
            UUID userId
    );

    List<EmergencyIncident> findAllByUserIdAndStatusOrderByTriggeredAtDesc(
            UUID userId,
            EmergencyIncidentStatus status
    );

    Optional<EmergencyIncident> findByIdAndUserId(
            UUID id,
            UUID userId
    );

    boolean existsByIdAndUserId(
            UUID id,
            UUID userId
    );
}
