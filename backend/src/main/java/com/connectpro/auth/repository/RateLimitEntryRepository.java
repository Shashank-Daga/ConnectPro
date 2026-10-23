package com.connectpro.auth.repository;

import com.connectpro.auth.entity.RateLimitEntry;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RateLimitEntryRepository extends JpaRepository<RateLimitEntry, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RateLimitEntry> findByKey(String key);

    @Modifying
    @Query(value = """
            INSERT INTO rate_limit_entries (
                id,
                key,
                attempt_count,
                window_started_at,
                created_at,
                updated_at
            )
            VALUES (
                gen_random_uuid(),
                :key,
                0,
                :windowStartedAt,
                :createdAt,
                :updatedAt
            )
            ON CONFLICT (key) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("key") String key,
            @Param("windowStartedAt") OffsetDateTime windowStartedAt,
            @Param("createdAt") OffsetDateTime createdAt,
            @Param("updatedAt") OffsetDateTime updatedAt
    );

    void deleteByWindowStartedAtBefore(OffsetDateTime cutoff);
}
