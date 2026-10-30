package pe.edu.upc.coldchain.alerts;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import pe.edu.upc.coldchain.alerts.config.ThermalRulesProperties;
import static org.assertj.core.api.Assertions.assertThat;

class ThermalRulesPropertiesTest {
    private static final String POINT = "b60d6bd0-e058-425d-9102-434b546e1284";
    private static final String PREFIX = "coldchain.alerts.rules." + POINT + ".";
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(RulesConfiguration.class);

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ThermalRulesProperties.class)
    static class RulesConfiguration { }

    @Test
    void noConfigurationDoesNotInventRules() {
        context.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx.getBean(ThermalRulesProperties.class).rules()).isEmpty();
        });
    }

    @Test
    void loadsExplicitUuidRuleWithDecimalPrecision() {
        context.withPropertyValues(PREFIX + "threshold-min=-25.125", PREFIX + "threshold-max=-15.0001")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    var rule = ctx.getBean(ThermalRulesProperties.class).rules().get(UUID.fromString(POINT));
                    assertThat(rule.thresholdMin()).isEqualByComparingTo(new BigDecimal("-25.125"));
                    assertThat(rule.thresholdMax()).isEqualByComparingTo(new BigDecimal("-15.0001"));
                });
    }

    @Test
    void allowsEqualLimits() {
        context.withPropertyValues(PREFIX + "threshold-min=0", PREFIX + "threshold-max=0")
                .run(ctx -> assertThat(ctx).hasNotFailed());
    }

    @Test
    void rejectsReversedLimitsAtStartup() {
        context.withPropertyValues(PREFIX + "threshold-min=8", PREFIX + "threshold-max=2")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void rejectsPartialRuleAtStartup() {
        context.withPropertyValues(PREFIX + "threshold-min=2").run(ctx -> assertThat(ctx).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"NaN", "Infinity", "1e131072", "100e2147483647", "1e-16384"})
    void rejectsUnstorableOrNonFiniteLimits(String min) {
        context.withPropertyValues(PREFIX + "threshold-min=" + min, PREFIX + "threshold-max=8")
                .run(ctx -> assertThat(ctx).hasFailed());
    }
}
