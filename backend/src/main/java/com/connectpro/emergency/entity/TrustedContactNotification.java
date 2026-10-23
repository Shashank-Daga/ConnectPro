package com.connectpro.emergency.entity;

import com.connectpro.emergency.model.TrustedContactNotificationStatus;
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
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "trusted_contact_notifications")
public class TrustedContactNotification {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private EmergencyIncident incident;

    @Column(name = "contact_id", nullable = false)
    private UUID contactId;

    @Column(name = "contact_name", nullable = false, length = 100)
    private String contactName;

    @Column(name = "recipient_phone_number", nullable = false, length = 20)
    private String recipientPhoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TrustedContactNotificationStatus status =
            TrustedContactNotificationStatus.PENDING;

    @Column(name = "delivery_attempts", nullable = false)
    private int deliveryAttempts;

    @Column(name = "last_error", length = 250)
    private String lastError;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected TrustedContactNotification() {
        // Required by JPA
    }

    public TrustedContactNotification(
            EmergencyIncident incident,
            UUID contactId,
            String contactName,
            String recipientPhoneNumber
    ) {
        this.incident = incident;
        this.contactId = contactId;
        this.contactName = contactName;
        this.recipientPhoneNumber = recipientPhoneNumber;
        this.status = TrustedContactNotificationStatus.PENDING;
        this.deliveryAttempts = 0;
    }

    public UUID getId() {
        return id;
    }

    public EmergencyIncident getIncident() {
        return incident;
    }

    public UUID getContactId() {
        return contactId;
    }

    public String getContactName() {
        return contactName;
    }

    public String getRecipientPhoneNumber() {
        return recipientPhoneNumber;
    }

    public TrustedContactNotificationStatus getStatus() {
        return status;
    }

    public int getDeliveryAttempts() {
        return deliveryAttempts;
    }

    public String getLastError() {
        return lastError;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void markAttemptStarted() {
        this.deliveryAttempts++;
        this.lastError = null;
    }

    public void markSent() {
        this.status = TrustedContactNotificationStatus.SENT;
        this.sentAt = OffsetDateTime.now();
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.status = TrustedContactNotificationStatus.FAILED;
        this.lastError = sanitizeError(error);
    }

    private String sanitizeError(String error) {
        if (error == null || error.isBlank()) {
            return "Delivery failed";
        }

        String sanitized = error
                .replaceAll("[\\r\\n\\t]", " ")
                .trim();

        return sanitized.length() > 250
                ? sanitized.substring(0, 250)
                : sanitized;
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
