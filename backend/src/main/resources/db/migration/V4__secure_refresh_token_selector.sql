-- ============================================================
-- ConnectPro - Secure refresh-token selector
-- Migration: V4
-- ============================================================

ALTER TABLE user_sessions
    ADD COLUMN refresh_token_selector VARCHAR(64);

CREATE UNIQUE INDEX uk_user_sessions_refresh_token_selector
    ON user_sessions(refresh_token_selector);
