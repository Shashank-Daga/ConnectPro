package com.connectpro.auth.service;

import com.connectpro.auth.repository.RateLimitEntryRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class RateLimitCleanupService {

    private final RateLimitEntryRepository rateLimitEntryRepository;

    public RateLimitCleanupService(
            RateLimitEntryRepository rateLimitEntryRepository
    ) {
        this.rateLimitEntryRepository = rateLimitEntryRepository;
    }

    @Transactional
    @Scheduled(fixedDelay = 3_600_000)
    public void cleanup() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusHours(24);

        rateLimitEntryRepository.deleteByWindowStartedAtBefore(cutoff);
    }
}
