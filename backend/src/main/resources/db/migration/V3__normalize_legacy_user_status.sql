-- ============================================================
-- ConnectPro - Normalize legacy user verification status
-- Migration: V3
-- ============================================================

UPDATE users
SET status = 'PENDING_VERIFICATION'
WHERE phone_verified_at IS NULL
   OR email_verified_at IS NULL;

ALTER TABLE users
    ALTER COLUMN phone_number SET NOT NULL;
