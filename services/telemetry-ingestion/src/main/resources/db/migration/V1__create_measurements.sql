CREATE TABLE measurements (
    id UUID PRIMARY KEY,
    monitoring_point_id UUID NOT NULL,
    temperature NUMERIC NOT NULL,
    timestamp TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    evaluation_status VARCHAR(24) NOT NULL,
    CONSTRAINT ck_measurements_evaluation_status
        CHECK (evaluation_status IN ('COMPLETED', 'PENDING', 'RULE_NOT_CONFIGURED'))
);

CREATE INDEX ix_measurements_point_timestamp_id
    ON measurements (monitoring_point_id, timestamp, id);
