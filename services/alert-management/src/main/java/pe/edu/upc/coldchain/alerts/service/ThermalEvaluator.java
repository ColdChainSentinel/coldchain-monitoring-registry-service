package pe.edu.upc.coldchain.alerts.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import pe.edu.upc.coldchain.alerts.config.ThermalRule;
import pe.edu.upc.coldchain.alerts.model.AlertType;

@Component
public class ThermalEvaluator {
    public AlertType violation(BigDecimal temperature, ThermalRule rule) {
        if (temperature.compareTo(rule.thresholdMin()) < 0) {
            return AlertType.LOW_TEMPERATURE;
        }
        if (temperature.compareTo(rule.thresholdMax()) > 0) {
            return AlertType.HIGH_TEMPERATURE;
        }
        return null;
    }
}
