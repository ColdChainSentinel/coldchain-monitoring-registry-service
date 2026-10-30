package pe.edu.upc.coldchain.alerts.config;

import java.math.BigDecimal;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import pe.edu.upc.coldchain.alerts.validation.PostgresNumeric;

public record ThermalRule(@NotNull @PostgresNumeric BigDecimal thresholdMin,
        @NotNull @PostgresNumeric BigDecimal thresholdMax) {
    @AssertTrue(message = "threshold-min debe ser menor o igual a threshold-max")
    public boolean isOrdered() {
        return thresholdMin == null || thresholdMax == null || thresholdMin.compareTo(thresholdMax) <= 0;
    }
}
