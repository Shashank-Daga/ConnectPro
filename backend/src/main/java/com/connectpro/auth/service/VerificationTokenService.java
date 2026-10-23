package com.connectpro.auth.service;

import com.connectpro.auth.entity.VerificationToken;
import com.connectpro.auth.exception.AuthenticationException;
import com.connectpro.auth.model.VerificationTokenType;
import com.connectpro.auth.repository.VerificationTokenRepository;
import com.connectpro.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class VerificationTokenService {

    private static final int VERIFICATION_CODE_LENGTH = 6;
    private static final long TOKEN_EXPIRATION_MINUTES = 15;

    private final VerificationTokenRepository verificationTokenRepository;
    private final RefreshTokenHashService refreshTokenHashService;

    private final SecureRandom secureRandom = new SecureRandom();

    public VerificationTokenService(
            VerificationTokenRepository verificationTokenRepository,
            RefreshTokenHashService refreshTokenHashService
    ) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.refreshTokenHashService = refreshTokenHashService;
    }

    @Transactional
    public String createToken(User user, VerificationTokenType type) {
        revokeExistingTokens(user, type);

        String rawToken = generateToken();

        String tokenHash = refreshTokenHashService.hash(rawToken);

        VerificationToken verificationToken = new VerificationToken(
                user,
                type,
                tokenHash,
                OffsetDateTime.now().plusMinutes(TOKEN_EXPIRATION_MINUTES)
        );

        verificationTokenRepository.save(verificationToken);

        return rawToken;
    }

    @Transactional
    public void verifyToken(
            User user,
            VerificationTokenType type,
            String rawToken
    ) {
        List<VerificationToken> tokens =
                verificationTokenRepository.findAllByUserIdAndType(
                        user.getId(),
                        type
                );

        VerificationToken matchingToken = tokens.stream()
                .filter(VerificationToken::isUsable)
                .filter(token ->
                        refreshTokenHashService.matches(
                                rawToken,
                                token.getTokenHash()
                        )
                )
                .findFirst()
                .orElseThrow(() ->
                        new AuthenticationException("Invalid verification token"));

        matchingToken.markUsed();
    }

    private void revokeExistingTokens(
            User user,
            VerificationTokenType type
    ) {
        verificationTokenRepository
                .findAllByUserIdAndType(user.getId(), type)
                .stream()
                .filter(VerificationToken::isUsable)
                .forEach(VerificationToken::markUsed);
    }

    private String generateToken() {
        int code = secureRandom.nextInt(1_000_000);

        return String.format(
                "%0" + VERIFICATION_CODE_LENGTH + "d",
                code
        );
    }
}
