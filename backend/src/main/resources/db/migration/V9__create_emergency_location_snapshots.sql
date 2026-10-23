-- ============================================================
-- ConnectPro - Emergency Location Snapshots
-- Migration: V9
-- ============================================================

CREATE TABLE emergency_location_snapshots (
                                              id UUID PRIMARY KEY,

                                              incident_id UUID NOT NULL,

                                              latitude NUMERIC(9, 6) NOT NULL,
                                              longitude NUMERIC(9, 6) NOT NULL,
                                              accuracy_meters NUMERIC(8, 2),

                                              captured_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                              CONSTRAINT fk_emergency_location_snapshots_incident
                                                  FOREIGN KEY (incident_id)
                                                      REFERENCES emergency_incidents(id)
                                                      ON DELETE CASCADE,

                                              CONSTRAINT uk_emergency_location_snapshots_incident
                                                  UNIQUE (incident_id),

                                              CONSTRAINT chk_emergency_location_snapshots_latitude
                                                  CHECK (
                                                      latitude >= -90
                                                          AND latitude <= 90
                                                      ),

                                              CONSTRAINT chk_emergency_location_snapshots_longitude
                                                  CHECK (
                                                      longitude >= -180
                                                          AND longitude <= 180
                                                      ),

                                              CONSTRAINT chk_emergency_location_snapshots_accuracy
                                                  CHECK (
                                                      accuracy_meters IS NULL
                                                          OR accuracy_meters >= 0
                                                      )
);

CREATE INDEX idx_emergency_location_snapshots_incident_id
    ON emergency_location_snapshots(incident_id);
