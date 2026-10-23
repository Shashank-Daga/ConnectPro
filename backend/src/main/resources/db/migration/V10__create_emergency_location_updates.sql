CREATE TABLE emergency_location_updates (
                                            id UUID PRIMARY KEY,
                                            incident_id UUID NOT NULL,
                                            latitude NUMERIC(9, 6) NOT NULL,
                                            longitude NUMERIC(9, 6) NOT NULL,
                                            accuracy_meters NUMERIC(8, 2),
                                            recorded_at TIMESTAMPTZ NOT NULL,
                                            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                            CONSTRAINT fk_emergency_location_updates_incident
                                                FOREIGN KEY (incident_id)
                                                    REFERENCES emergency_incidents(id)
                                                    ON DELETE CASCADE,

                                            CONSTRAINT chk_emergency_location_updates_latitude
                                                CHECK (
                                                    latitude >= -90
                                                        AND latitude <= 90
                                                    ),

                                            CONSTRAINT chk_emergency_location_updates_longitude
                                                CHECK (
                                                    longitude >= -180
                                                        AND longitude <= 180
                                                    ),

                                            CONSTRAINT chk_emergency_location_updates_accuracy
                                                CHECK (
                                                    accuracy_meters IS NULL
                                                        OR accuracy_meters >= 0
                                                    )
);

CREATE INDEX idx_emergency_location_updates_incident_recorded_at
    ON emergency_location_updates(incident_id, recorded_at DESC);

CREATE INDEX idx_emergency_location_updates_incident_created_at
    ON emergency_location_updates(incident_id, created_at DESC);
