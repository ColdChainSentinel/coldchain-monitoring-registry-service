package pe.edu.upc.coldchain.alerts.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import pe.edu.upc.coldchain.alerts.model.ThermalRuleEntity;

public record ThermalRuleResponse(
        UUID monitoringPointId,
        BigDecimal thresholdMin,
        BigDecimal thresholdMax,
        String unit,
        Instant updatedAt) {

    public static ThermalRuleResponse from(ThermalRuleEntity entity) {
        return new ThermalRuleResponse(
                entity.getMonitoringPointId(),
                entity.getThresholdMin(),
                entity.getThresholdMax(),
                entity.getUnit(),
                entity.getUpdatedAt());
    }
}
