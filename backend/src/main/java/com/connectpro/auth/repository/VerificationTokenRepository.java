package com.connectpro.auth.repository;

import com.connectpro.auth.entity.VerificationToken;
import com.connectpro.auth.model.VerificationTokenType;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<VerificationToken> findAllByUserIdAndType(
            UUID userId,
            VerificationTokenType type
    );

    Optional<VerificationToken> findByUserIdAndTypeAndTokenHash(
            UUID userId,
            VerificationTokenType type,
            String tokenHash
    );
}
