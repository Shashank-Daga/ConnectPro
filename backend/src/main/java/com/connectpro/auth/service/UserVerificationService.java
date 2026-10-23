package com.connectpro.auth.service;

import com.connectpro.auth.exception.AuthenticationException;
import com.connectpro.auth.model.VerificationTokenType;
import com.connectpro.user.entity.User;
import com.connectpro.user.repository.UserRepository;
import com.connectpro.common.web.ClientIpResolver;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserVerificationService {

    private final UserRepository userRepository;
    private final VerificationTokenService verificationTokenService;
    private final RateLimitService rateLimitService;
    private final ClientIpResolver clientIpResolver;

    public UserVerificationService(
            UserRepository userRepository,
            VerificationTokenService verificationTokenService,
            RateLimitService rateLimitService,
            ClientIpResolver clientIpResolver
    ) {
        this.userRepository = userRepository;
        this.verificationTokenService = verificationTokenService;
        this.rateLimitService = rateLimitService;
        this.clientIpResolver = clientIpResolver;
    }

    @Transactional
    public void verifyPhone(String phoneNumber, String token, HttpServletRequest httpRequest) {
        String normalizedPhoneNumber = phoneNumber.trim();

        String clientIp = clientIpResolver.resolve(httpRequest);

        rateLimitService.checkAndRecord(
                "verify-phone:" + normalizedPhoneNumber,
                RateLimitPolicy.VERIFICATION_MAX_ATTEMPTS,
                RateLimitPolicy.VERIFICATION_WINDOW
        );

        rateLimitService.checkAndRecord(
                "verify-phone:ip:" + clientIp,
                RateLimitPolicy.VERIFICATION_IP_MAX_ATTEMPTS,
                RateLimitPolicy.VERIFICATION_IP_WINDOW
        );

        User user = findUserByPhoneNumber(normalizedPhoneNumber);

        verificationTokenService.verifyToken(
                user,
                VerificationTokenType.PHONE_VERIFICATION,
                token
        );

        user.markPhoneVerified();
    }

    @Transactional
    public void verifyEmail(String email, String token, HttpServletRequest httpRequest) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        String clientIp = clientIpResolver.resolve(httpRequest);

        rateLimitService.checkAndRecord(
                "verify-email:" + normalizedEmail,
                RateLimitPolicy.VERIFICATION_MAX_ATTEMPTS,
                RateLimitPolicy.VERIFICATION_WINDOW
        );

        rateLimitService.checkAndRecord(
                "verify-email:ip:" + clientIp,
                RateLimitPolicy.VERIFICATION_IP_MAX_ATTEMPTS,
                RateLimitPolicy.VERIFICATION_IP_WINDOW
        );

        User user = findUserByEmail(normalizedEmail);

        verificationTokenService.verifyToken(
                user,
                VerificationTokenType.EMAIL_VERIFICATION,
                token
        );

        user.markEmailVerified();
    }

    private User findUserByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber.trim())
                .orElseThrow(() ->
                        new AuthenticationException("Invalid verification request"));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() ->
                        new AuthenticationException("Invalid verification request"));
    }
}
