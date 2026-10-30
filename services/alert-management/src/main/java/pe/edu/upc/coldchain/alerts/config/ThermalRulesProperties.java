package pe.edu.upc.coldchain.alerts.config;

import java.util.Map;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "coldchain.alerts")
public record ThermalRulesProperties(@Valid Map<UUID, @NotNull ThermalRule> rules) {
    public ThermalRulesProperties {
        rules = rules == null ? Map.of() : Map.copyOf(rules);
    }
}
