
CREATE TABLE trusted_contact_notifications (
                                               id UUID PRIMARY KEY,

                                               incident_id UUID NOT NULL,
                                               contact_id UUID NOT NULL,

                                               contact_name VARCHAR(100) NOT NULL,
                                               recipient_phone_number VARCHAR(20) NOT NULL,

                                               status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                               delivery_attempts INTEGER NOT NULL DEFAULT 0,
                                               last_error VARCHAR(250),
                                               sent_at TIMESTAMPTZ,

                                               created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                               updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                               CONSTRAINT fk_trusted_contact_notifications_incident
                                                   FOREIGN KEY (incident_id)
                                                       REFERENCES emergency_incidents(id)
                                                       ON DELETE CASCADE,

                                               CONSTRAINT uq_trusted_contact_notifications_incident_contact
                                                   UNIQUE (incident_id, contact_id),

                                               CONSTRAINT chk_trusted_contact_notifications_status
                                                   CHECK (status IN ('PENDING', 'SENT', 'FAILED')),

                                               CONSTRAINT chk_trusted_contact_notifications_attempts
                                                   CHECK (delivery_attempts >= 0)
);

CREATE INDEX idx_trusted_contact_notifications_incident_created
    ON trusted_contact_notifications(incident_id, created_at ASC);

CREATE INDEX idx_trusted_contact_notifications_pending
    ON trusted_contact_notifications(status, created_at ASC);
