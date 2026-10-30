package pe.edu.upc.coldchain.alerts;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.upc.coldchain.alerts.config.ThermalRule;
import pe.edu.upc.coldchain.alerts.model.AlertType;
import pe.edu.upc.coldchain.alerts.service.ThermalEvaluator;
import static org.assertj.core.api.Assertions.assertThat;

class ThermalEvaluatorTest {
    private final ThermalEvaluator evaluator = new ThermalEvaluator();
    private final ThermalRule rule = new ThermalRule(new BigDecimal("2.0"), new BigDecimal("8.00"));

    @ParameterizedTest
    @ValueSource(strings = {"2", "2.000", "5", "8.0000", "7.999999999999999999"})
    void includesBothLimitsAndUsesExactDecimals(String value) {
        assertThat(evaluator.violation(new BigDecimal(value), rule)).isNull();
    }

    @ParameterizedTest
    @CsvSource({"1.999999999999999999,LOW_TEMPERATURE", "-20,LOW_TEMPERATURE",
        "8.000000000000000001,HIGH_TEMPERATURE", "20,HIGH_TEMPERATURE"})
    void classifiesOnlyActualViolations(String value, AlertType type) {
        assertThat(evaluator.violation(new BigDecimal(value), rule)).isEqualTo(type);
    }

    @Test
    void supportsAnExplicitNegativeRangeWithoutUniversalLimits() {
        var frozen = new ThermalRule(new BigDecimal("-25"), new BigDecimal("-15"));
        assertThat(evaluator.violation(new BigDecimal("-20"), frozen)).isNull();
        assertThat(evaluator.violation(new BigDecimal("-14"), frozen)).isEqualTo(AlertType.HIGH_TEMPERATURE);
    }

    @Test
    void equalLimitsAllowOnlyThatExactValue() {
        var exact = new ThermalRule(BigDecimal.ZERO, BigDecimal.ZERO);
        assertThat(evaluator.violation(BigDecimal.ZERO, exact)).isNull();
        assertThat(evaluator.violation(new BigDecimal("-0.01"), exact)).isEqualTo(AlertType.LOW_TEMPERATURE);
        assertThat(evaluator.violation(new BigDecimal("0.01"), exact)).isEqualTo(AlertType.HIGH_TEMPERATURE);
    }
}
