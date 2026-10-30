package pe.edu.upc.coldchain.alerts.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.coldchain.alerts.validation.PostgresNumeric;

public record EvaluateMeasurementRequest(
        @NotNull(message = "El identificador de medición es obligatorio") UUID measurementId,
        @NotNull(message = "El punto de monitoreo es obligatorio") UUID monitoringPointId,
        @NotNull(message = "La temperatura es obligatoria") @PostgresNumeric
        @Schema(type = "number", description = "Número JSON Celsius, no string") BigDecimal temperature,
        @NotNull(message = "La fecha de medición es obligatoria")
        @Schema(type = "string", format = "date-time", description = "RFC 3339 con zona; UTC a microsegundos")
        Instant timestamp) {
    public EvaluateMeasurementRequest normalized() {
        return new EvaluateMeasurementRequest(measurementId, monitoringPointId,
                temperature.stripTrailingZeros(), timestamp.truncatedTo(ChronoUnit.MICROS));
    }
}
