CREATE TABLE alerts (
    id UUID PRIMARY KEY,
    monitoring_point_id UUID NOT NULL,
    measurement_id UUID NOT NULL,
    temperature NUMERIC NOT NULL,
    threshold_min NUMERIC NOT NULL,
    threshold_max NUMERIC NOT NULL,
    status VARCHAR(8) NOT NULL,
    type VARCHAR(24) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_alerts_measurement UNIQUE (measurement_id),
    CONSTRAINT ck_alerts_thresholds CHECK (threshold_min <= threshold_max),
    CONSTRAINT ck_alerts_status CHECK (status = 'OPEN'),
    CONSTRAINT ck_alerts_type_temperature CHECK (
        (type = 'LOW_TEMPERATURE' AND temperature < threshold_min)
        OR (type = 'HIGH_TEMPERATURE' AND temperature > threshold_max))
);

CREATE INDEX ix_alerts_point_created_id ON alerts (monitoring_point_id, created_at, id);

CREATE TABLE measurement_evaluations (
    measurement_id UUID PRIMARY KEY,
    monitoring_point_id UUID NOT NULL,
    temperature NUMERIC NOT NULL,
    timestamp TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    alert_id UUID UNIQUE REFERENCES alerts(id),
    CONSTRAINT ck_evaluations_outcome CHECK (
        (outcome = 'NO_ALERT' AND alert_id IS NULL)
        OR (outcome = 'ALERT_CREATED' AND alert_id IS NOT NULL))
);
