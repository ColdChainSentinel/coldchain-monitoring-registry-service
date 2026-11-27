ALTER TABLE monitoring_points
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE devices (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL,
    monitoring_point_id UUID NOT NULL REFERENCES monitoring_points(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_devices_code UNIQUE (code)
);

CREATE INDEX idx_devices_point_id ON devices(monitoring_point_id);
