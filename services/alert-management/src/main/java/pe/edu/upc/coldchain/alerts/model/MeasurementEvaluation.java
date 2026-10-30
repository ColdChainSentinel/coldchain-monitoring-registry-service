package pe.edu.upc.coldchain.alerts.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "measurement_evaluations")
public class MeasurementEvaluation {
    @Id
    @Column(name = "measurement_id")
    private UUID measurementId;
    @Column(name = "monitoring_point_id", nullable = false)
    private UUID monitoringPointId;
    @Column(nullable = false, columnDefinition = "numeric")
    private BigDecimal temperature;
    @Column(nullable = false)
    private Instant timestamp;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EvaluationOutcome outcome;
    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "alert_id", unique = true)
    private Alert alert;

    protected MeasurementEvaluation() { }

    public MeasurementEvaluation(UUID measurementId, UUID monitoringPointId, BigDecimal temperature,
            Instant timestamp, Alert alert) {
        this.measurementId = measurementId;
        this.monitoringPointId = monitoringPointId;
        this.temperature = temperature;
        this.timestamp = timestamp;
        this.alert = alert;
        this.outcome = alert == null ? EvaluationOutcome.NO_ALERT : EvaluationOutcome.ALERT_CREATED;
    }

    public UUID getMeasurementId() { return measurementId; }
    public UUID getMonitoringPointId() { return monitoringPointId; }
    public BigDecimal getTemperature() { return temperature; }
    public Instant getTimestamp() { return timestamp; }
    public EvaluationOutcome getOutcome() { return outcome; }
    public Alert getAlert() { return alert; }
}
