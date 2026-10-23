-- ============================================================
-- ConnectPro - Rate-limit entries
-- Migration: V6
-- ============================================================

CREATE TABLE rate_limit_entries (
                                    id UUID PRIMARY KEY,
                                    key VARCHAR(255) NOT NULL,
                                    attempt_count INTEGER NOT NULL,
                                    window_started_at TIMESTAMPTZ NOT NULL,
                                    created_at TIMESTAMPTZ NOT NULL,
                                    updated_at TIMESTAMPTZ NOT NULL,

                                    CONSTRAINT uk_rate_limit_entries_key UNIQUE (key),
                                    CONSTRAINT chk_rate_limit_entries_attempt_count
                                        CHECK (attempt_count >= 0)
);

CREATE INDEX idx_rate_limit_entries_window_started_at
    ON rate_limit_entries(window_started_at);
