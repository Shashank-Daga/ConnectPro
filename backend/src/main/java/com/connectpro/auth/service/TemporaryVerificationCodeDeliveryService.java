
package com.connectpro.auth.service;

import com.connectpro.auth.model.VerificationTokenType;
import com.connectpro.user.entity.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Temporary fallback used when email and SMS delivery have not
 * yet been integrated.
 *
 * This implementation allows the application to start without
 * an external verification-code delivery provider.
 *
 * Verification codes are deliberately not logged or delivered.
 * Replace this implementation when production delivery is ready.
 */
@Service
@Profile("!dev & !test")
public class TemporaryVerificationCodeDeliveryService
        implements VerificationCodeDeliveryService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    TemporaryVerificationCodeDeliveryService.class
            );

    @Override
    public void deliver(
            User user,
            VerificationTokenType type,
            String verificationCode
    ) {
        log.warn(
                "Verification-code delivery is not configured. "
                        + "Code was not delivered. type={}, userId={}",
                type,
                user.getId()
        );
    }
}
