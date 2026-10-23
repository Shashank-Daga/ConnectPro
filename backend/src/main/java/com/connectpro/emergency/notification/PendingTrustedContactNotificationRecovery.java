package com.connectpro.emergency.notification;

import com.connectpro.emergency.repository.TrustedContactNotificationRepository;
import com.connectpro.emergency.service.TrustedContactNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class PendingTrustedContactNotificationRecovery {

    private static final Logger log =
            LoggerFactory.getLogger(
                    PendingTrustedContactNotificationRecovery.class
            );

    private final TrustedContactNotificationRepository notificationRepository;
    private final TrustedContactNotificationService notificationService;

    public PendingTrustedContactNotificationRecovery(
            TrustedContactNotificationRepository notificationRepository,
            TrustedContactNotificationService notificationService
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelayString = "${connectpro.notifications.recovery-delay-ms:30000}")
    public void recoverPendingNotifications() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(2);

        List<UUID> incidentIds =
                notificationRepository
                        .findPendingIncidentIdsCreatedBefore(cutoff);

        for (UUID incidentId : incidentIds) {
            try {
                notificationService.dispatchPendingForIncident(incidentId);
            } catch (RuntimeException exception) {
                log.error(
                        "Notification recovery failed for incident {}. "
                                + "exceptionType={}",
                        incidentId,
                        exception.getClass().getSimpleName()
                );
            }
        }
    }
}
