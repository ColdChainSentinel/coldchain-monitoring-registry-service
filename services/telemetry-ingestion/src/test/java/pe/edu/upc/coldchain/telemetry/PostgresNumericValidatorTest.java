package pe.edu.upc.coldchain.telemetry;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.upc.coldchain.telemetry.validation.PostgresNumericValidator;
import static org.assertj.core.api.Assertions.assertThat;

class PostgresNumericValidatorTest {
    private final PostgresNumericValidator validator = new PostgresNumericValidator();

    @ParameterizedTest
    @ValueSource(strings = {"0", "-300", "100000", "1e131071", "1e-16383", "-0.000000123456789", "0e2147483647"})
    void acceptsNumericCapacityWithoutThermalLimits(String value) {
        assertThat(validator.isValid(new BigDecimal(value), null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1e131072", "1e-16384", "1e999999999", "1e-999999999", "100e2147483647"})
    void rejectsOverflowWithoutExpandingHugeExponent(String value) {
        assertThat(validator.isValid(new BigDecimal(value), null)).isFalse();
    }
}
