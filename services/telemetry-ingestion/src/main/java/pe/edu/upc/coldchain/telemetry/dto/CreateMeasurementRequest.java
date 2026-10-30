package pe.edu.upc.coldchain.telemetry.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.coldchain.telemetry.validation.PostgresNumeric;

public record CreateMeasurementRequest(
        @NotNull(message = "El punto de monitoreo es obligatorio") UUID monitoringPointId,
        @NotNull(message = "La temperatura es obligatoria")
        @PostgresNumeric
        @Schema(type = "number", description = "Número JSON Celsius; sin umbral térmico universal") BigDecimal temperature,
        @NotNull(message = "La fecha de medición es obligatoria")
        @Schema(type = "string", format = "date-time", description = "RFC 3339 con zona; normalizado a microsegundos UTC")
        Instant timestamp) { }
