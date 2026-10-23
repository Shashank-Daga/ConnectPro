package com.connectpro.emergency.notification;

import com.connectpro.emergency.service.TrustedContactNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TrustedContactAlertListener {

    private static final Logger log =
            LoggerFactory.getLogger(TrustedContactAlertListener.class);

    private final TrustedContactNotificationService notificationService;

    public TrustedContactAlertListener(
            TrustedContactNotificationService notificationService
    ) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(TrustedContactAlertRequested event) {
        try {
            notificationService.dispatchPendingForIncident(
                    event.incidentId()
            );
        } catch (RuntimeException exception) {
            // A delivery-processing problem must not undo incident activation.
            log.error(
                    "Failed to process trusted-contact alerts for incident {}",
                    event.incidentId(),
                    exception
            );
        }
    }
}
