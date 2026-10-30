package pe.edu.upc.coldchain.telemetry.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.coldchain.telemetry.model.EvaluationStatus;
import pe.edu.upc.coldchain.telemetry.model.Measurement;

@Schema(requiredProperties = {"id", "monitoringPointId", "temperature", "timestamp", "evaluationStatus"})
public record MeasurementResponse(UUID id, UUID monitoringPointId, BigDecimal temperature,
        Instant timestamp, EvaluationStatus evaluationStatus) {
    public static MeasurementResponse from(Measurement measurement) {
        return new MeasurementResponse(measurement.getId(), measurement.getMonitoringPointId(),
                measurement.getTemperature(), measurement.getTimestamp(), measurement.getEvaluationStatus());
    }
}
