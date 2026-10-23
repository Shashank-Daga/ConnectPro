-- ============================================================
-- ConnectPro - Enforce refresh-token selector
-- Migration: V5
-- ============================================================

DELETE FROM user_sessions
WHERE refresh_token_selector IS NULL;

ALTER TABLE user_sessions
    ALTER COLUMN refresh_token_selector SET NOT NULL;
