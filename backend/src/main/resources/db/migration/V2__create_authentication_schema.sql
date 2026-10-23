-- ============================================================
-- ConnectPro - Authentication & Security Schema
-- Migration: V2
-- ============================================================


-- ============================================================
-- USERS
-- Extend the existing V1 users table.
-- ============================================================

ALTER TABLE users
    ADD COLUMN phone_number VARCHAR(20),
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    ADD COLUMN phone_verified_at TIMESTAMPTZ,
    ADD COLUMN email_verified_at TIMESTAMPTZ;


-- ============================================================
-- MIGRATE EXISTING V1 ACCOUNT STATE
--
-- V1:
--   enabled = TRUE  -> ACTIVE
--   enabled = FALSE -> DEACTIVATED
--
-- Existing V1 accounts cannot automatically be considered
-- phone/email verified, so verification timestamps remain NULL.
-- ============================================================

UPDATE users
SET status = CASE
                 WHEN enabled = TRUE THEN 'ACTIVE'
                 ELSE 'DEACTIVATED'
    END;


-- ============================================================
-- USER CREDENTIALS
--
-- Stores authentication credentials separately from users.
--
-- credential_hash contains the existing V1 password hash when
-- migrating an existing account.
--
-- New PINs will be stored as Argon2id hashes by the application.
-- ============================================================

CREATE TABLE user_credentials (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    credential_type VARCHAR(30) NOT NULL,
    credential_hash VARCHAR(255) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_credentials_user
      FOREIGN KEY (user_id)
          REFERENCES users(id)
          ON DELETE CASCADE,

    CONSTRAINT uk_user_credentials_user_type
      UNIQUE (user_id, credential_type),

    CONSTRAINT chk_user_credentials_type
      CHECK (
          credential_type IN ('PIN')
          )
);


-- ============================================================
-- MIGRATE EXISTING V1 PASSWORD HASHES
--
-- V1 stored password_hash directly in users.
-- Move those hashes into user_credentials before removing the
-- old column.
-- ============================================================

INSERT INTO user_credentials (
    id,
    user_id,
    credential_type,
    credential_hash,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    id,
    'PIN',
    password_hash,
    created_at,
    updated_at
FROM users
WHERE password_hash IS NOT NULL;


-- ============================================================
-- USER SESSIONS
--
-- One user can have multiple authenticated sessions/devices.
--
-- Only a hash of the refresh token is stored.
-- ============================================================

CREATE TABLE user_sessions (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    refresh_token_hash VARCHAR(255) NOT NULL,

    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMPTZ,

    CONSTRAINT fk_user_sessions_user
       FOREIGN KEY (user_id)
           REFERENCES users(id)
           ON DELETE CASCADE,

    CONSTRAINT uk_user_sessions_refresh_token_hash
       UNIQUE (refresh_token_hash)
);


-- ============================================================
-- VERIFICATION TOKENS
--
-- Used for:
--   PHONE_VERIFICATION
--   EMAIL_VERIFICATION
--   PIN_RESET
--
-- Only a hash of the token/OTP is stored.
-- ============================================================

CREATE TABLE verification_tokens (
     id UUID PRIMARY KEY,

     user_id UUID NOT NULL,

     type VARCHAR(30) NOT NULL,
     token_hash VARCHAR(255) NOT NULL,

     expires_at TIMESTAMPTZ NOT NULL,
     used_at TIMESTAMPTZ,

     created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

     CONSTRAINT fk_verification_tokens_user
         FOREIGN KEY (user_id)
             REFERENCES users(id)
             ON DELETE CASCADE,

     CONSTRAINT chk_verification_tokens_type
         CHECK (
             type IN (
                      'PHONE_VERIFICATION',
                      'EMAIL_VERIFICATION',
                      'PIN_RESET'
                 )
             )
);


-- ============================================================
-- USERS CONSTRAINTS
-- ============================================================

ALTER TABLE users
    ADD CONSTRAINT uk_users_phone_number
        UNIQUE (phone_number);

ALTER TABLE users
    ADD CONSTRAINT chk_users_status
        CHECK (
            status IN (
                       'PENDING_VERIFICATION',
                       'ACTIVE',
                       'SUSPENDED',
                       'LOCKED',
                       'DEACTIVATED'
                )
            );


-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_user_sessions_user_id
    ON user_sessions(user_id);

CREATE INDEX idx_user_sessions_expires_at
    ON user_sessions(expires_at);

CREATE INDEX idx_verification_tokens_user_id
    ON verification_tokens(user_id);

CREATE INDEX idx_verification_tokens_expires_at
    ON verification_tokens(expires_at);

CREATE INDEX idx_verification_tokens_user_type
    ON verification_tokens(user_id, type);


-- ============================================================
-- REMOVE V1 AUTHENTICATION FIELDS
--
-- Authentication credentials now live in user_credentials.
-- ============================================================

ALTER TABLE users
DROP COLUMN password_hash;

ALTER TABLE users
DROP COLUMN enabled;
