package pe.edu.upc.coldchain.alerts.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.coldchain.alerts.model.Alert;
import pe.edu.upc.coldchain.alerts.model.AlertStatus;
import pe.edu.upc.coldchain.alerts.model.AlertType;

@Schema(requiredProperties = {"id", "monitoringPointId", "measurementId", "temperature",
    "thresholdMin", "thresholdMax", "status", "createdAt"})
public record AlertResponse(UUID id, UUID monitoringPointId, UUID measurementId, BigDecimal temperature,
        BigDecimal thresholdMin, BigDecimal thresholdMax, AlertStatus status, Instant createdAt,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) AlertType type) {
    public static AlertResponse from(Alert alert) {
        return new AlertResponse(alert.getId(), alert.getMonitoringPointId(), alert.getMeasurementId(),
                alert.getTemperature(), alert.getThresholdMin(), alert.getThresholdMax(), alert.getStatus(),
                alert.getCreatedAt(), alert.getType());
    }
}
