-- ============================================================
-- ConnectPro - Emergency Contacts
-- Migration: V7
-- ============================================================

CREATE TABLE emergency_contacts (
                                    id UUID PRIMARY KEY,

                                    user_id UUID NOT NULL,

                                    name VARCHAR(100) NOT NULL,
                                    phone_number VARCHAR(20) NOT NULL,
                                    relationship VARCHAR(50) NOT NULL,

                                    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                                    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT fk_emergency_contacts_user
                                        FOREIGN KEY (user_id)
                                            REFERENCES users(id)
                                            ON DELETE CASCADE,

                                    CONSTRAINT chk_emergency_contacts_status
                                        CHECK (
                                            status IN ('ACTIVE', 'INACTIVE')
                                            )
);

CREATE INDEX idx_emergency_contacts_user_id
    ON emergency_contacts(user_id);

CREATE INDEX idx_emergency_contacts_user_status
    ON emergency_contacts(user_id, status);
