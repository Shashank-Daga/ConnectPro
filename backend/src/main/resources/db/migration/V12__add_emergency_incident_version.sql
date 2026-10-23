ALTER TABLE emergency_incidents
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
