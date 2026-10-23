package com.connectpro.emergency.repository;

import com.connectpro.emergency.entity.TrustedContactNotification;
import com.connectpro.emergency.model.TrustedContactNotificationStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;

public interface TrustedContactNotificationRepository
        extends JpaRepository<TrustedContactNotification, UUID> {

    List<TrustedContactNotification>
    findAllByIncidentIdOrderByCreatedAtAsc(UUID incidentId);

    List<TrustedContactNotification>
    findAllByIncidentIdAndStatusOrderByCreatedAtAsc(
            UUID incidentId,
            TrustedContactNotificationStatus status
    );

    boolean existsByIncidentIdAndContactId(
            UUID incidentId,
            UUID contactId
    );

    @Query(value = """
        SELECT DISTINCT incident_id
        FROM trusted_contact_notifications
        WHERE status = 'PENDING'
          AND created_at < :cutoff
        ORDER BY incident_id
        LIMIT 100
        """, nativeQuery = true)
    List<UUID> findPendingIncidentIdsCreatedBefore(
            @Param("cutoff") OffsetDateTime cutoff
    );
}
