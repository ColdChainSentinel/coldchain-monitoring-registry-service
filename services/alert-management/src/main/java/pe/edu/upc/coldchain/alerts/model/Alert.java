package pe.edu.upc.coldchain.alerts.model;

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
@Table(name = "alerts")
public class Alert {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(name = "monitoring_point_id", nullable = false)
    private UUID monitoringPointId;
    @Column(name = "measurement_id", nullable = false, unique = true)
    private UUID measurementId;
    @Column(nullable = false, columnDefinition = "numeric")
    private BigDecimal temperature;
    @Column(name = "threshold_min", nullable = false, columnDefinition = "numeric")
    private BigDecimal thresholdMin;
    @Column(name = "threshold_max", nullable = false, columnDefinition = "numeric")
    private BigDecimal thresholdMax;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private AlertStatus status;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AlertType type;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Alert() { }

    public Alert(UUID monitoringPointId, UUID measurementId, BigDecimal temperature,
            BigDecimal thresholdMin, BigDecimal thresholdMax, AlertType type, Instant createdAt) {
        this.monitoringPointId = monitoringPointId;
        this.measurementId = measurementId;
        this.temperature = temperature;
        this.thresholdMin = thresholdMin;
        this.thresholdMax = thresholdMax;
        this.type = type;
        this.createdAt = createdAt;
        this.status = AlertStatus.OPEN;
    }

    public UUID getId() { return id; }
    public UUID getMonitoringPointId() { return monitoringPointId; }
    public UUID getMeasurementId() { return measurementId; }
    public BigDecimal getTemperature() { return temperature; }
    public BigDecimal getThresholdMin() { return thresholdMin; }
    public BigDecimal getThresholdMax() { return thresholdMax; }
    public AlertStatus getStatus() { return status; }
    public AlertType getType() { return type; }
    public Instant getCreatedAt() { return createdAt; }
}
