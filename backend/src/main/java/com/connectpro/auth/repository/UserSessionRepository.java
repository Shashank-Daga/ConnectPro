package com.connectpro.auth.repository;

import com.connectpro.auth.entity.UserSession;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserSession> findByRefreshTokenSelectorAndRevokedAtIsNull(
            String refreshTokenSelector
    );

    List<UserSession> findAllByUserId(UUID userId);

    List<UserSession> findAllByUserIdAndRevokedAtIsNull(UUID userId);
}
