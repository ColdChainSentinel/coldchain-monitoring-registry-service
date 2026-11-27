package pe.edu.upc.coldchain.alerts.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import pe.edu.upc.coldchain.alerts.validation.PostgresNumeric;

public record ConfigureThermalRuleRequest(
        @NotNull(message = "El límite mínimo es obligatorio")
        @PostgresNumeric
        BigDecimal thresholdMin,
        @NotNull(message = "El límite máximo es obligatorio")
        @PostgresNumeric
        BigDecimal thresholdMax) {

    @AssertTrue(message = "La temperatura mínima debe ser estrictamente menor que la máxima")
    public boolean isStrictlyOrdered() {
        return thresholdMin == null || thresholdMax == null || thresholdMin.compareTo(thresholdMax) < 0;
    }
}
