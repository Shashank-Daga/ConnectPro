package com.connectpro.auth.service;

import com.connectpro.auth.dto.AuthResponse;
import com.connectpro.auth.dto.LoginRequest;
import com.connectpro.auth.dto.RefreshTokenRequest;
import com.connectpro.auth.dto.RegisterRequest;
import com.connectpro.auth.entity.UserCredential;
import com.connectpro.auth.entity.UserSession;
import com.connectpro.auth.exception.AuthenticationException;
import com.connectpro.auth.model.CredentialType;
import com.connectpro.auth.repository.UserCredentialRepository;
import com.connectpro.auth.repository.UserSessionRepository;
import com.connectpro.user.entity.User;
import com.connectpro.user.model.UserStatus;
import com.connectpro.user.repository.UserRepository;
import com.connectpro.auth.model.VerificationTokenType;
import com.connectpro.auth.dto.RequestPinResetRequest;
import com.connectpro.auth.dto.ResetPinRequest;
import com.connectpro.common.web.ClientIpResolver;
import com.connectpro.config.JwtProperties;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final UserSessionRepository userSessionRepository;
    private final PinHashService pinHashService;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenHashService refreshTokenHashService;
    private final VerificationCodeDeliveryService verificationCodeDeliveryService;
    private final VerificationTokenService verificationTokenService;
    private final RateLimitService rateLimitService;
    private final ClientIpResolver clientIpResolver;
    private final JwtProperties jwtProperties;

    public AuthenticationService(
            UserRepository userRepository,
            UserCredentialRepository userCredentialRepository,
            UserSessionRepository userSessionRepository,
            PinHashService pinHashService,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            RefreshTokenHashService refreshTokenHashService,
            VerificationCodeDeliveryService verificationCodeDeliveryService,
            VerificationTokenService verificationTokenService,
            RateLimitService rateLimitService,
            ClientIpResolver clientIpResolver,
            JwtProperties jwtProperties
    ) {
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.userSessionRepository = userSessionRepository;
        this.pinHashService = pinHashService;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenHashService = refreshTokenHashService;
        this.verificationCodeDeliveryService = verificationCodeDeliveryService;
        this.verificationTokenService = verificationTokenService;
        this.rateLimitService = rateLimitService;
        this.clientIpResolver = clientIpResolver;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public void register(RegisterRequest request, HttpServletRequest httpRequest) {
        String normalizedPhone = request.phoneNumber().trim();
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        String clientIp = clientIpResolver.resolve(httpRequest);

        rateLimitService.checkAndRecord(
                "register:phone:" + normalizedPhone,
                RateLimitPolicy.REGISTRATION_MAX_ATTEMPTS,
                RateLimitPolicy.REGISTRATION_WINDOW
        );

        rateLimitService.checkAndRecord(
                "register:email:" + normalizedEmail,
                RateLimitPolicy.REGISTRATION_MAX_ATTEMPTS,
                RateLimitPolicy.REGISTRATION_WINDOW
        );

        rateLimitService.checkAndRecord(
                "register:ip:" + clientIp,
                RateLimitPolicy.REGISTRATION_IP_MAX_ATTEMPTS,
                RateLimitPolicy.REGISTRATION_IP_WINDOW
        );

        if (userRepository.existsByPhoneNumber(normalizedPhone)
                || userRepository.existsByEmail(normalizedEmail)) {
            throw new AuthenticationException("Unable to create account");
        }

        User user = new User(normalizedPhone, normalizedEmail);
        userRepository.save(user);

        String pinHash = pinHashService.hash(request.pin());

        UserCredential credential = new UserCredential(
                user,
                CredentialType.PIN,
                pinHash
        );

        userCredentialRepository.save(credential);

        String phoneVerificationCode =
                verificationTokenService.createToken(
                        user,
                        VerificationTokenType.PHONE_VERIFICATION
                );

        String emailVerificationCode =
                verificationTokenService.createToken(
                        user,
                        VerificationTokenType.EMAIL_VERIFICATION
                );

        verificationCodeDeliveryService.deliver(
                user,
                VerificationTokenType.PHONE_VERIFICATION,
                phoneVerificationCode
        );

        verificationCodeDeliveryService.deliver(
                user,
                VerificationTokenType.EMAIL_VERIFICATION,
                emailVerificationCode
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String normalizedPhone = request.phoneNumber().trim();

        String clientIp = clientIpResolver.resolve(httpRequest);

        rateLimitService.checkAndRecord(
                "login:phone:" + normalizedPhone,
                RateLimitPolicy.LOGIN_MAX_ATTEMPTS,
                RateLimitPolicy.LOGIN_WINDOW
        );

        rateLimitService.checkAndRecord(
                "login:ip:" + clientIp,
                RateLimitPolicy.LOGIN_IP_MAX_ATTEMPTS,
                RateLimitPolicy.LOGIN_IP_WINDOW
        );

        User user = userRepository.findByPhoneNumber(normalizedPhone)
                .orElseThrow(() ->
                        new AuthenticationException("Invalid credentials"));

        UserCredential credential = userCredentialRepository
                .findByUserIdAndCredentialType(
                        user.getId(),
                        CredentialType.PIN
                )
                .orElseThrow(() ->
                        new AuthenticationException("Invalid credentials"));

        boolean pinMatches = pinHashService.matches(
                request.pin(),
                credential.getCredentialHash()
        );

        if (!pinMatches) {
            throw new AuthenticationException("Invalid credentials");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationException("Invalid credentials");
        }

        RefreshTokenService.RefreshToken refreshToken =
                refreshTokenService.generate();

        String refreshTokenHash =
                refreshTokenHashService.hash(refreshToken.secret());

        OffsetDateTime refreshTokenExpiresAt =
                OffsetDateTime.now().plusSeconds(jwtProperties.refreshTokenExpiration());

        UserSession session = new UserSession(
                user,
                refreshToken.selector(),
                refreshTokenHash,
                refreshTokenExpiresAt
        );

        userSessionRepository.save(session);

        String accessToken = jwtTokenService.generateAccessToken(
                user.getId(),
                session.getId()
        );

        return new AuthResponse(
                accessToken,
                refreshToken.value(),
                "Bearer",
                jwtProperties.accessTokenExpiration()
        );
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String rawRefreshToken = request.refreshToken();

        String[] tokenParts = rawRefreshToken.split("\\.", -1);

        if (tokenParts.length != 2
                || tokenParts[0].isBlank()
                || tokenParts[1].isBlank()) {
            throw new AuthenticationException("Invalid refresh token");
        }

        String selector = tokenParts[0];
        String secret = tokenParts[1];

        String clientIp = clientIpResolver.resolve(httpRequest);

        rateLimitService.checkAndRecord(
                "refresh:ip:" + clientIp,
                RateLimitPolicy.REFRESH_IP_MAX_ATTEMPTS,
                RateLimitPolicy.REFRESH_IP_WINDOW
        );

        rateLimitService.checkAndRecord(
                "refresh:" + selector,
                RateLimitPolicy.REFRESH_MAX_ATTEMPTS,
                RateLimitPolicy.REFRESH_WINDOW
        );

        UserSession session = userSessionRepository
                .findByRefreshTokenSelectorAndRevokedAtIsNull(selector)
                .orElseThrow(() ->
                        new AuthenticationException("Invalid refresh token"));

        if (!session.isUsable() || !refreshTokenHashService.matches(
                secret, session.getRefreshTokenHash()
        )) {
            throw new AuthenticationException("Invalid refresh token");
        }

        session.markUsed();

        User user = session.getUser();

        if (user.getStatus() != UserStatus.ACTIVE) {
            session.revoke();
            throw new AuthenticationException("Invalid refresh token");
        }

        session.revoke();

        RefreshTokenService.RefreshToken newRefreshToken =
                refreshTokenService.generate();

        String newRefreshTokenHash =
                refreshTokenHashService.hash(newRefreshToken.secret());

        OffsetDateTime newRefreshTokenExpiresAt =
                OffsetDateTime.now().plusSeconds(jwtProperties.refreshTokenExpiration());

        UserSession newSession = new UserSession(
                user,
                newRefreshToken.selector(),
                newRefreshTokenHash,
                newRefreshTokenExpiresAt
        );

        userSessionRepository.save(newSession);

        String accessToken = jwtTokenService.generateAccessToken(
                user.getId(),
                newSession.getId()
        );

        return new AuthResponse(
                accessToken,
                newRefreshToken.value(),
                "Bearer",
                jwtProperties.accessTokenExpiration()
        );
    }

    @Transactional
    public void requestPinReset(RequestPinResetRequest request, HttpServletRequest httpRequest) {
        String normalizedIdentifier = request.identifier().trim().toLowerCase(Locale.ROOT);

        String clientIp = clientIpResolver.resolve(httpRequest);

        rateLimitService.checkAndRecord(
                "pin-reset-request:" + normalizedIdentifier,
                RateLimitPolicy.PIN_RESET_REQUEST_MAX_ATTEMPTS,
                RateLimitPolicy.PIN_RESET_REQUEST_WINDOW
        );

        rateLimitService.checkAndRecord(
                "pin-reset-request:ip:" + clientIp,
                RateLimitPolicy.PIN_RESET_REQUEST_IP_MAX_ATTEMPTS,
                RateLimitPolicy.PIN_RESET_REQUEST_IP_WINDOW
        );

        User user = userRepository.findByPhoneNumber(normalizedIdentifier)
                .or(() -> userRepository.findByEmail(
                        normalizedIdentifier.toLowerCase(Locale.ROOT)
                ))
                .orElse(null);

        if (user == null) {
            return;
        }

        String verificationCode =
                verificationTokenService.createToken(
                        user,
                        VerificationTokenType.PIN_RESET
                );

        verificationCodeDeliveryService.deliver(
                user,
                VerificationTokenType.PIN_RESET,
                verificationCode
        );
    }

    @Transactional
    public void resetPin(ResetPinRequest request, HttpServletRequest httpRequest) {
        String normalizedIdentifier = request.identifier().trim().toLowerCase(Locale.ROOT);

        String clientIp = clientIpResolver.resolve(httpRequest);

        rateLimitService.checkAndRecord(
                "pin-reset:" + normalizedIdentifier,
                RateLimitPolicy.PIN_RESET_MAX_ATTEMPTS,
                RateLimitPolicy.PIN_RESET_WINDOW
        );

        rateLimitService.checkAndRecord(
                "pin-reset:ip:" + clientIp,
                RateLimitPolicy.PIN_RESET_IP_MAX_ATTEMPTS,
                RateLimitPolicy.PIN_RESET_IP_WINDOW
        );

        User user = userRepository.findByPhoneNumber(normalizedIdentifier)
                .or(() -> userRepository.findByEmail(
                        normalizedIdentifier.toLowerCase(Locale.ROOT)
                ))
                .orElseThrow(() ->
                        new AuthenticationException("Invalid reset request"));

        verificationTokenService.verifyToken(
                user,
                VerificationTokenType.PIN_RESET,
                request.verificationCode()
        );

        UserCredential credential = userCredentialRepository
                .findByUserIdAndCredentialType(
                        user.getId(),
                        CredentialType.PIN
                )
                .orElseThrow(() ->
                        new AuthenticationException("Invalid reset request"));

        String newPinHash = pinHashService.hash(request.newPin());

        credential.updateCredentialHash(newPinHash);

        userSessionRepository
                .findAllByUserIdAndRevokedAtIsNull(user.getId())
                .forEach(UserSession::revoke);
    }

    @Transactional
    public void logout(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();

        String sessionId = jwt.getClaimAsString("session_id");
        String subject = jwt.getSubject();

        if (sessionId == null || sessionId.isBlank()
                || subject == null || subject.isBlank()) {
            throw new AuthenticationException("Invalid authentication");
        }

        UUID sessionUuid;
        UUID userUuid;

        try {
            sessionUuid = UUID.fromString(sessionId);
            userUuid = UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new AuthenticationException("Invalid authentication");
        }

        UserSession session = userSessionRepository.findById(sessionUuid)
                .orElseThrow(() ->
                        new AuthenticationException("Invalid authentication"));

        if (!session.getUser().getId().equals(userUuid)) {
            throw new AuthenticationException("Invalid authentication");
        }

        if (!session.isRevoked()) {
            session.revoke();
        }
    }
}
