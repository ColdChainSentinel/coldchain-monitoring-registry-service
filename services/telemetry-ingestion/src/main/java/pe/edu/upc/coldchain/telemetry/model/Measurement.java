package pe.edu.upc.coldchain.telemetry.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "measurements")
public class Measurement {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(name = "monitoring_point_id", nullable = false)
    private UUID monitoringPointId;
    @Column(nullable = false, columnDefinition = "numeric")
    private BigDecimal temperature;
    @Column(nullable = false)
    private Instant timestamp;
    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_status", nullable = false, length = 24)
    private EvaluationStatus evaluationStatus;

    protected Measurement() { }

    public Measurement(UUID monitoringPointId, BigDecimal temperature, Instant timestamp) {
        this.monitoringPointId = monitoringPointId;
        this.temperature = temperature;
        this.timestamp = timestamp;
        this.evaluationStatus = EvaluationStatus.PENDING;
    }

    public UUID getId() { return id; }
    public UUID getMonitoringPointId() { return monitoringPointId; }
    public BigDecimal getTemperature() { return temperature; }
    public Instant getTimestamp() { return timestamp; }
    public EvaluationStatus getEvaluationStatus() { return evaluationStatus; }

    public void recordEvaluation(EvaluationStatus status) {
        this.evaluationStatus = status;
    }
}
