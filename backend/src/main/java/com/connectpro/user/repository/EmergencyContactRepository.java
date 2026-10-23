package com.connectpro.user.repository;

import com.connectpro.user.entity.EmergencyContact;
import com.connectpro.user.model.EmergencyContactStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmergencyContactRepository
        extends JpaRepository<EmergencyContact, UUID> {

    List<EmergencyContact> findAllByUserIdOrderByCreatedAtAsc(UUID userId);

    List<EmergencyContact> findAllByUserIdAndStatusOrderByCreatedAtAsc(
            UUID userId,
            EmergencyContactStatus status
    );

    Optional<EmergencyContact> findByIdAndUserId(
            UUID id,
            UUID userId
    );

    boolean existsByIdAndUserId(
            UUID id,
            UUID userId
    );
}
