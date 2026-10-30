package pe.edu.upc.coldchain.alerts.validation;

import java.math.BigDecimal;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PostgresNumericValidator implements ConstraintValidator<PostgresNumeric, BigDecimal> {
    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        long integerDigits = (long) value.precision() - value.scale();
        if (value.signum() != 0 && integerDigits > 131072) {
            return false;
        }
        return value.stripTrailingZeros().scale() <= 16383;
    }
}
