package pe.edu.upc.coldchain.telemetry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import pe.edu.upc.coldchain.telemetry.model.EvaluationStatus;
import pe.edu.upc.coldchain.telemetry.model.Measurement;
import pe.edu.upc.coldchain.telemetry.repository.MeasurementRepository;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("smoke")
class MeasurementRepositoryTest {
    @Autowired
    private MeasurementRepository repository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void roundTripsUuidExactDecimalUtcMicrosecondsAndStatus() {
        UUID point = UUID.randomUUID();
        var timestamp = Instant.parse("2026-10-29T10:00:00.123456Z");
        var temperature = new BigDecimal("-12.123456789123456789");
        var measurement = repository.saveAndFlush(new Measurement(point, temperature, timestamp));
        UUID id = measurement.getId();
        entityManager.clear();
        var found = repository.findById(id).orElseThrow();
        assertThat(found.getMonitoringPointId()).isEqualTo(point);
        assertThat(found.getTemperature()).isEqualByComparingTo(temperature);
        assertThat(found.getTimestamp()).isEqualTo(timestamp);
        assertThat(found.getEvaluationStatus()).isEqualTo(EvaluationStatus.PENDING);
    }

    @Test
    void filtersPointAndSortsByTimestampThenUuid() {
        UUID point = UUID.randomUUID();
        var early = Instant.parse("2026-10-29T09:00:00Z");
        var late = Instant.parse("2026-10-29T10:00:00Z");
        var last = repository.saveAndFlush(new Measurement(point, BigDecimal.ONE, late));
        var a = repository.saveAndFlush(new Measurement(point, BigDecimal.ZERO, early));
        var b = repository.saveAndFlush(new Measurement(point, BigDecimal.TEN, early));
        repository.saveAndFlush(new Measurement(UUID.randomUUID(), BigDecimal.ZERO, early));
        entityManager.clear();
        var results = repository.findByMonitoringPointIdOrderByTimestampAscIdAsc(point);
        assertThat(results).hasSize(3);
        // UUID string order is unsigned byte order (PostgreSQL); UUID.compareTo is signed.
        var firstIds = java.util.stream.Stream.of(a.getId(), b.getId())
                .sorted(java.util.Comparator.comparing(UUID::toString)).toList();
        assertThat(results).extracting(Measurement::getId)
                .containsExactly(firstIds.get(0), firstIds.get(1), last.getId());
    }

    @Test
    void missingPointReturnsNoRows() {
        assertThat(repository.findByMonitoringPointIdOrderByTimestampAscIdAsc(UUID.randomUUID())).isEmpty();
    }
}
