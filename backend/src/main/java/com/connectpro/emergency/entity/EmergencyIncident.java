package com.connectpro.emergency.entity;

import com.connectpro.emergency.model.EmergencyCategory;
import com.connectpro.emergency.model.EmergencyIncidentStatus;
import com.connectpro.user.entity.User;
import com.connectpro.common.exception.InvalidStateTransitionException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "emergency_incidents")
public class EmergencyIncident {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private EmergencyCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmergencyIncidentStatus status;

    @Column(name = "triggered_at", nullable = false)
    private OffsetDateTime triggeredAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected EmergencyIncident() {
        // Required by JPA
    }

    public EmergencyIncident(
            User user,
            EmergencyCategory category
    ) {
        this.user = user;
        this.category = category;
        this.status = EmergencyIncidentStatus.CREATED;
        this.triggeredAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public EmergencyCategory getCategory() {
        return category;
    }

    public EmergencyIncidentStatus getStatus() {
        return status;
    }

    public OffsetDateTime getTriggeredAt() {
        return triggeredAt;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void activate() {
        if (status != EmergencyIncidentStatus.CREATED) {
            throw new InvalidStateTransitionException(
                    "Incident can only be activated from CREATED state"
            );
        }

        this.status = EmergencyIncidentStatus.ACTIVE;
    }

    public void resolve() {
        if (status != EmergencyIncidentStatus.ACTIVE) {
            throw new InvalidStateTransitionException(
                    "Incident can only be resolved from ACTIVE state"
            );
        }

        this.status = EmergencyIncidentStatus.RESOLVED;
        this.resolvedAt = OffsetDateTime.now();
    }

    public void cancel() {
        if (status != EmergencyIncidentStatus.CREATED && status != EmergencyIncidentStatus.ACTIVE) {
            throw new InvalidStateTransitionException(
                    "Incident cannot be cancelled from its current state"
            );
        }

        this.status = EmergencyIncidentStatus.CANCELLED;
        this.resolvedAt = OffsetDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.triggeredAt == null) {
            this.triggeredAt = now;
        }

        if (this.status == null) {
            this.status = EmergencyIncidentStatus.CREATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
