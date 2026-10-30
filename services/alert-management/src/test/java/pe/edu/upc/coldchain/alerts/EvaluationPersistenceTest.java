package pe.edu.upc.coldchain.alerts;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import pe.edu.upc.coldchain.alerts.config.ThermalRule;
import pe.edu.upc.coldchain.alerts.dto.EvaluateMeasurementRequest;
import pe.edu.upc.coldchain.alerts.model.AlertType;
import pe.edu.upc.coldchain.alerts.model.EvaluationOutcome;
import pe.edu.upc.coldchain.alerts.repository.AlertRepository;
import pe.edu.upc.coldchain.alerts.service.EvaluationStore;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("smoke")
@Import({EvaluationStore.class, EvaluationPersistenceTest.ClockConfiguration.class})
class EvaluationPersistenceTest {
    private static final Instant NOW = Instant.parse("2026-10-29T11:00:00.123456789Z");
    @Autowired
    private EvaluationStore store;
    @Autowired
    private AlertRepository alerts;
    @Autowired
    private EntityManager entityManager;

    @TestConfiguration
    static class ClockConfiguration {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Test
    void savesAndReloadsAlertAndEvaluationWithExactSnapshots() {
        UUID point = UUID.randomUUID();
        var rule = new ThermalRule(new BigDecimal("-25.123456789123456789"), new BigDecimal("-15.125"));
        var input = new EvaluateMeasurementRequest(UUID.randomUUID(), point, new BigDecimal("-30.123456789123456789"),
                Instant.parse("2026-10-29T10:00:00.123456Z"));
        var result = store.insert(input, rule, AlertType.LOW_TEMPERATURE);
        entityManager.clear();
        var saved = store.find(input.measurementId()).orElseThrow();
        assertThat(saved.matches(input)).isTrue();
        assertThat(saved.response().outcome()).isEqualTo(EvaluationOutcome.ALERT_CREATED);
        var alert = saved.response().alert();
        assertThat(alert.id()).isEqualTo(result.alert().id());
        assertThat(alert.measurementId()).isEqualTo(input.measurementId());
        assertThat(alert.temperature()).isEqualByComparingTo(input.temperature());
        assertThat(alert.thresholdMin()).isEqualByComparingTo(rule.thresholdMin());
        assertThat(alert.thresholdMax()).isEqualByComparingTo(rule.thresholdMax());
        assertThat(alert.type()).isEqualTo(AlertType.LOW_TEMPERATURE);
        assertThat(alert.status().name()).isEqualTo("OPEN");
        assertThat(alert.createdAt()).isEqualTo(Instant.parse("2026-10-29T11:00:00.123456Z"));
    }

    @Test
    void persistsNoAlertResultForFutureIdempotency() {
        var input = new EvaluateMeasurementRequest(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO,
                Instant.parse("2026-10-29T10:00:00Z"));
        store.insert(input, new ThermalRule(BigDecimal.ZERO, BigDecimal.ONE), null);
        entityManager.clear();
        var saved = store.find(input.measurementId()).orElseThrow();
        assertThat(saved.response().outcome()).isEqualTo(EvaluationOutcome.NO_ALERT);
        assertThat(saved.response().alert()).isNull();
        assertThat(store.list(input.monitoringPointId())).isEmpty();
    }

    @Test
    void filtersAndSortsAlertListWithStableUuidTieBreak() {
        UUID point = UUID.randomUUID();
        var rule = new ThermalRule(BigDecimal.ZERO, BigDecimal.ONE);
        var a = store.insert(new EvaluateMeasurementRequest(UUID.randomUUID(), point, BigDecimal.TEN, NOW),
                rule, AlertType.HIGH_TEMPERATURE);
        var b = store.insert(new EvaluateMeasurementRequest(UUID.randomUUID(), point, BigDecimal.TEN, NOW),
                rule, AlertType.HIGH_TEMPERATURE);
        store.insert(new EvaluateMeasurementRequest(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, NOW),
                rule, AlertType.HIGH_TEMPERATURE);
        entityManager.clear();
        var expected = Stream.of(a.alert().id(), b.alert().id()).sorted(Comparator.comparing(UUID::toString)).toList();
        assertThat(store.list(point)).extracting(alert -> alert.id()).containsExactlyElementsOf(expected);
        assertThat(alerts.findByMonitoringPointIdOrderByCreatedAtAscIdAsc(UUID.randomUUID())).isEmpty();
    }
}
