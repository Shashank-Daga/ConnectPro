package com.connectpro.auth.service;

import com.connectpro.auth.model.VerificationTokenType;
import com.connectpro.user.entity.User;

public interface VerificationCodeDeliveryService {

    void deliver(
            User user,
            VerificationTokenType type,
            String verificationCode
    );
}
