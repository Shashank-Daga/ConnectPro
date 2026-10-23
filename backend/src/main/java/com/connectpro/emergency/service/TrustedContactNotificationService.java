package com.connectpro.emergency.service;

import com.connectpro.common.exception.ResourceNotFoundException;
import com.connectpro.common.security.AuthenticatedUser;
import com.connectpro.emergency.dto.TrustedContactNotificationResponse;
import com.connectpro.emergency.entity.EmergencyIncident;
import com.connectpro.emergency.entity.TrustedContactNotification;
import com.connectpro.emergency.model.TrustedContactNotificationStatus;
import com.connectpro.emergency.notification.SmsDeliveryProvider;
import com.connectpro.emergency.notification.TrustedContactAlertRequested;
import com.connectpro.emergency.repository.EmergencyIncidentRepository;
import com.connectpro.emergency.repository.TrustedContactNotificationRepository;
import com.connectpro.user.entity.EmergencyContact;
import com.connectpro.user.model.EmergencyContactStatus;
import com.connectpro.user.repository.EmergencyContactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TrustedContactNotificationService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    TrustedContactNotificationService.class
            );

    private final EmergencyContactRepository emergencyContactRepository;
    private final EmergencyIncidentRepository emergencyIncidentRepository;
    private final TrustedContactNotificationRepository notificationRepository;
    private final SmsDeliveryProvider smsDeliveryProvider;
    private final ApplicationEventPublisher applicationEventPublisher;

    public TrustedContactNotificationService(
            EmergencyContactRepository emergencyContactRepository,
            EmergencyIncidentRepository emergencyIncidentRepository,
            TrustedContactNotificationRepository notificationRepository,
            SmsDeliveryProvider smsDeliveryProvider,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.emergencyContactRepository = emergencyContactRepository;
        this.emergencyIncidentRepository = emergencyIncidentRepository;
        this.notificationRepository = notificationRepository;
        this.smsDeliveryProvider = smsDeliveryProvider;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Called inside the incident activation transaction.
     * Recipient records commit atomically with the ACTIVE state.
     */
    @Transactional
    public void queueAlertsForActivation(EmergencyIncident incident) {
        UUID incidentId = incident.getId();
        UUID userId = incident.getUser().getId();

        List<EmergencyContact> activeContacts =
                emergencyContactRepository
                        .findAllByUserIdAndStatusOrderByCreatedAtAsc(
                                userId,
                                EmergencyContactStatus.ACTIVE
                        );

        List<TrustedContactNotification> newNotifications =
                new ArrayList<>();

        for (EmergencyContact contact : activeContacts) {
            boolean alreadyExists =
                    notificationRepository.existsByIncidentIdAndContactId(
                            incidentId,
                            contact.getId()
                    );

            if (!alreadyExists) {
                newNotifications.add(
                        new TrustedContactNotification(
                                incident,
                                contact.getId(),
                                contact.getName(),
                                contact.getPhoneNumber()
                        )
                );
            }
        }

        if (!newNotifications.isEmpty()) {
            notificationRepository.saveAll(newNotifications);
        }

        boolean hasPendingNotifications =
                !notificationRepository
                        .findAllByIncidentIdAndStatusOrderByCreatedAtAsc(
                                incidentId,
                                TrustedContactNotificationStatus.PENDING
                        )
                        .isEmpty();

        if (hasPendingNotifications) {
            applicationEventPublisher.publishEvent(
                    new TrustedContactAlertRequested(incidentId)
            );
        }
    }

    /**
     * Invoked by the AFTER_COMMIT listener in a new transaction.
     */
    @Transactional
    public void dispatchPendingForIncident(UUID incidentId) {
        EmergencyIncident incident = emergencyIncidentRepository
                .findById(incidentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Emergency incident not found"
                        ));

        List<TrustedContactNotification> pendingNotifications =
                notificationRepository
                        .findAllByIncidentIdAndStatusOrderByCreatedAtAsc(
                                incidentId,
                                TrustedContactNotificationStatus.PENDING
                        );

        String message = buildEmergencyMessage(incident);

        for (TrustedContactNotification notification
                : pendingNotifications) {
            notification.markAttemptStarted();

            try {
                smsDeliveryProvider.sendSms(
                        notification.getRecipientPhoneNumber(),
                        message
                );

                notification.markSent();

            } catch (RuntimeException exception) {
                notification.markFailed("Delivery provider error");

                log.warn(
                        "Trusted-contact SMS delivery failed. "
                                + "incidentId={}, notificationId={}, exceptionType={}",
                        incidentId,
                        notification.getId(),
                        exception.getClass().getSimpleName()
                );
            }
        }

        notificationRepository.saveAll(pendingNotifications);
    }

    @Transactional(readOnly = true)
    public List<TrustedContactNotificationResponse> getForIncident(
            Authentication authentication,
            UUID incidentId
    ) {
        UUID userId = AuthenticatedUser.getUserId(authentication);

        // Ownership check happens before any notification data is returned.
        emergencyIncidentRepository
                .findByIdAndUserId(incidentId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Emergency incident not found"
                        ));

        return notificationRepository
                .findAllByIncidentIdOrderByCreatedAtAsc(incidentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String buildEmergencyMessage(EmergencyIncident incident) {
        return "ConnectPro emergency alert: A "
                + incident.getCategory().name()
                + " emergency has been activated. Incident reference: "
                + incident.getId()
                + ". Please contact the person who shared this alert "
                + "with you.";
    }

    private TrustedContactNotificationResponse toResponse(
            TrustedContactNotification notification
    ) {
        return new TrustedContactNotificationResponse(
                notification.getId(),
                notification.getContactName(),
                maskPhoneNumber(
                        notification.getRecipientPhoneNumber()
                ),
                notification.getStatus(),
                notification.getDeliveryAttempts(),
                notification.getSentAt(),
                notification.getCreatedAt()
        );
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() <= 4) {
            return "****";
        }

        return "*".repeat(phoneNumber.length() - 4)
                + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
