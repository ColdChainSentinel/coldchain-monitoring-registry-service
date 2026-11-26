package pe.edu.upc.coldchain.telemetry.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.coldchain.telemetry.validation.PostgresNumeric;

public record CreateMeasurementRequest(
        @NotNull(message = "El punto de monitoreo es obligatorio") UUID monitoringPointId,
        @NotNull(message = "La temperatura es obligatoria")
        @PostgresNumeric
        @DecimalMin(value = "-40", message = "La temperatura debe estar entre -40 °C y 50 °C")
        @DecimalMax(value = "50", message = "La temperatura debe estar entre -40 °C y 50 °C")
        @Schema(type = "number", description = "Número JSON en °C dentro del rango operativo [-40, 50]", minimum = "-40", maximum = "50") BigDecimal temperature,
        @NotNull(message = "La fecha de medición es obligatoria")
        @Schema(type = "string", format = "date-time", description = "RFC 3339 con zona; normalizado a microsegundos UTC")
        Instant timestamp) { }
