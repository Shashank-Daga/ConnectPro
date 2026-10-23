-- ============================================================
-- ConnectPro - Emergency Incidents
-- Migration: V8
-- ============================================================

CREATE TABLE emergency_incidents (
                                     id UUID PRIMARY KEY,

                                     user_id UUID NOT NULL,

                                     category VARCHAR(30) NOT NULL,
                                     status VARCHAR(20) NOT NULL DEFAULT 'CREATED',

                                     triggered_at TIMESTAMPTZ NOT NULL,
                                     resolved_at TIMESTAMPTZ,

                                     created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     CONSTRAINT fk_emergency_incidents_user
                                         FOREIGN KEY (user_id)
                                             REFERENCES users(id)
                                             ON DELETE CASCADE,

                                     CONSTRAINT chk_emergency_incidents_category
                                         CHECK (
                                             category IN (
                                                          'MEDICAL',
                                                          'ACCIDENT',
                                                          'PERSONAL_THREAT',
                                                          'FIRE',
                                                          'CRIME',
                                                          'MISSING_PERSON',
                                                          'OTHER'
                                                 )
                                             ),

                                     CONSTRAINT chk_emergency_incidents_status
                                         CHECK (
                                             status IN (
                                                        'CREATED',
                                                        'ACTIVE',
                                                        'RESOLVED',
                                                        'CANCELLED'
                                                 )
                                             ),

                                     CONSTRAINT chk_emergency_incidents_resolved_at
                                         CHECK (
                                             (
                                                 status IN ('RESOLVED', 'CANCELLED')
                                                     AND resolved_at IS NOT NULL
                                                 )
                                                 OR
                                             (
                                                 status IN ('CREATED', 'ACTIVE')
                                                     AND resolved_at IS NULL
                                                 )
                                             )
);

CREATE INDEX idx_emergency_incidents_user_id
    ON emergency_incidents(user_id);

CREATE INDEX idx_emergency_incidents_user_status
    ON emergency_incidents(user_id, status);

CREATE INDEX idx_emergency_incidents_user_triggered_at
    ON emergency_incidents(user_id, triggered_at DESC);
