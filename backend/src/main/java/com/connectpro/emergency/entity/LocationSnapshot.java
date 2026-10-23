package com.connectpro.emergency.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "emergency_location_snapshots")
public class LocationSnapshot {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "incident_id",
            nullable = false,
            unique = true
    )
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

    @Column(name = "captured_at", nullable = false)
    private OffsetDateTime capturedAt;

    protected LocationSnapshot() {
        // Required by JPA
    }

    public LocationSnapshot(
            EmergencyIncident incident,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal accuracyMeters,
            OffsetDateTime capturedAt
    ) {
        this.incident = incident;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyMeters = accuracyMeters;
        this.capturedAt = capturedAt;
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

    public OffsetDateTime getCapturedAt() {
        return capturedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.capturedAt == null) {
            this.capturedAt = OffsetDateTime.now();
        }
    }
}
