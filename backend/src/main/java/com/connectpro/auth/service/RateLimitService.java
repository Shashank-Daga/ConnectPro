package com.connectpro.auth.service;

import com.connectpro.auth.entity.RateLimitEntry;
import com.connectpro.auth.repository.RateLimitEntryRepository;
import com.connectpro.common.exception.RateLimitExceededException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;

@Service
public class RateLimitService {

    private final RateLimitEntryRepository rateLimitEntryRepository;

    public RateLimitService(RateLimitEntryRepository rateLimitEntryRepository) {
        this.rateLimitEntryRepository = rateLimitEntryRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkAndRecord(String key, int maxAttempts, Duration window) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Rate limit key must not be blank");
        }

        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("Rate limit maxAttempts must be greater than zero");
        }

        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Rate limit window must be positive");
        }

        OffsetDateTime now = OffsetDateTime.now();

        rateLimitEntryRepository.insertIfAbsent(
                key,
                now,
                now,
                now
        );

        RateLimitEntry entry = rateLimitEntryRepository.findByKey(key)
                .orElseThrow(() ->
                        new IllegalStateException("Rate limit entry was not created"));

        if (Duration.between(entry.getWindowStartedAt(), now).compareTo(window) >= 0) {
            entry.resetWindow(now);
        }

        if (entry.getAttemptCount() >= maxAttempts) {
            throw new RateLimitExceededException("Rate limit exceeded");
        }

        entry.incrementAttempts();
    }
}
