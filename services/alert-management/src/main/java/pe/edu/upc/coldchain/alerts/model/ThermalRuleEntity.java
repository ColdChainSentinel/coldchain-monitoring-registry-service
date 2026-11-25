package pe.edu.upc.coldchain.alerts.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "thermal_rules")
public class ThermalRuleEntity {
    @Id
    @Column(name = "monitoring_point_id")
    private UUID monitoringPointId;

    @Column(name = "threshold_min", nullable = false)
    private BigDecimal thresholdMin;

    @Column(name = "threshold_max", nullable = false)
    private BigDecimal thresholdMax;

    @Column(nullable = false, length = 8)
    private String unit;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ThermalRuleEntity() {
    }

    public ThermalRuleEntity(UUID monitoringPointId, BigDecimal thresholdMin, BigDecimal thresholdMax,
            String unit, Instant updatedAt) {
        this.monitoringPointId = monitoringPointId;
        this.thresholdMin = thresholdMin;
        this.thresholdMax = thresholdMax;
        this.unit = unit;
        this.updatedAt = updatedAt;
    }

    public UUID getMonitoringPointId() {
        return monitoringPointId;
    }

    public BigDecimal getThresholdMin() {
        return thresholdMin;
    }

    public BigDecimal getThresholdMax() {
        return thresholdMax;
    }

    public String getUnit() {
        return unit;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateRange(BigDecimal min, BigDecimal max, Instant updatedAt) {
        this.thresholdMin = min;
        this.thresholdMax = max;
        this.updatedAt = updatedAt;
    }
}
