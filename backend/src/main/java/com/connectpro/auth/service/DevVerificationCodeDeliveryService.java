package com.connectpro.auth.service;

import com.connectpro.auth.model.VerificationTokenType;
import com.connectpro.user.entity.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"dev", "test"})
public class DevVerificationCodeDeliveryService
        implements VerificationCodeDeliveryService {

    private static final Logger log =
            LoggerFactory.getLogger(DevVerificationCodeDeliveryService.class);

    @Override
    public void deliver(
            User user,
            VerificationTokenType type,
            String verificationCode
    ) {
        log.info(
                "DEV verification code generated: type={}, userId={}, code={}",
                type,
                user.getId(),
                verificationCode
        );
    }
}
