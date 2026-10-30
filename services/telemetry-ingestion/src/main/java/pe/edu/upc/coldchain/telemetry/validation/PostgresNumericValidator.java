package pe.edu.upc.coldchain.telemetry.validation;

import java.math.BigDecimal;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PostgresNumericValidator implements ConstraintValidator<PostgresNumeric, BigDecimal> {
    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // @NotNull reports required fields separately.
        }
        // Check the exponent before normalization, which could itself overflow scale.
        long integerDigits = (long) value.precision() - value.scale();
        if (value.signum() != 0 && integerDigits > 131072) {
            return false;
        }
        return value.stripTrailingZeros().scale() <= 16383;
    }
}
