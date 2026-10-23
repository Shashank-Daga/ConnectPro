package com.connectpro.emergency.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "emergency_location_updates")
public class LocationUpdate {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private EmergencyIncident incident;

    @Column(
            name = "latitude",
            nullable = false,
            precision = 9,
            scale = 6
    )
    private BigDecimal latitude;

    @Column(
            name = "longitude",
            nullable = false,
            precision = 9,
            scale = 6
    )
    private BigDecimal longitude;

    @Column(
            name = "accuracy_meters",
            precision = 8,
            scale = 2
    )
    private BigDecimal accuracyMeters;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected LocationUpdate() {
        // Required by JPA
    }

    public LocationUpdate(
            EmergencyIncident incident,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal accuracyMeters,
            OffsetDateTime recordedAt
    ) {
        this.incident = incident;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyMeters = accuracyMeters;
        this.recordedAt = recordedAt;
    }

    public UUID getId() {
        return id;
    }

    public EmergencyIncident getIncident() {
        return incident;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public BigDecimal getAccuracyMeters() {
        return accuracyMeters;
    }

    public OffsetDateTime getRecordedAt() {
        return recordedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }

        if (this.recordedAt == null) {
            this.recordedAt = this.createdAt;
        }
    }
}
