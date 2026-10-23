package com.connectpro.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "rate_limit_entries")
public class RateLimitEntry {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "key", nullable = false, unique = true, length = 255)
    private String key;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "window_started_at", nullable = false)
    private OffsetDateTime windowStartedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected RateLimitEntry() {
        // Required by JPA
    }

    public RateLimitEntry(
            String key,
            OffsetDateTime windowStartedAt
    ) {
        this.key = key;
        this.windowStartedAt = windowStartedAt;
        this.attemptCount = 0;
    }

    public UUID getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public OffsetDateTime getWindowStartedAt() {
        return windowStartedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void incrementAttempts() {
        this.attemptCount++;
    }

    public void resetWindow(OffsetDateTime newWindowStartedAt) {
        this.windowStartedAt = newWindowStartedAt;
        this.attemptCount = 0;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
