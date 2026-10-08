CREATE TABLE monitoring_points (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    type VARCHAR(20) NOT NULL,
    CONSTRAINT uk_monitoring_points_code UNIQUE (code),
    CONSTRAINT ck_monitoring_points_type CHECK (type IN ('ALMACENAMIENTO', 'TRANSPORTE'))
);
