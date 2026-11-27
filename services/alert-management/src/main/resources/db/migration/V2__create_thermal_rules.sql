CREATE TABLE thermal_rules (
    monitoring_point_id UUID PRIMARY KEY,
    threshold_min NUMERIC NOT NULL,
    threshold_max NUMERIC NOT NULL,
    unit VARCHAR(8) NOT NULL DEFAULT '°C',
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_thermal_rules_range CHECK (threshold_min < threshold_max)
);
