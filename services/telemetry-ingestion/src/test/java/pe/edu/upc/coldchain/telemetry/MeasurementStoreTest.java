package pe.edu.upc.coldchain.telemetry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import pe.edu.upc.coldchain.telemetry.dto.CreateMeasurementRequest;
import pe.edu.upc.coldchain.telemetry.model.EvaluationStatus;
import pe.edu.upc.coldchain.telemetry.model.Measurement;
import pe.edu.upc.coldchain.telemetry.repository.MeasurementRepository;
import pe.edu.upc.coldchain.telemetry.service.MeasurementStore;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeasurementStoreTest {
    @Mock
    private MeasurementRepository repository;
    @InjectMocks
    private MeasurementStore store;

    @Test
    void truncatesTimestampAndPreservesDecimalWithoutRounding() {
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var request = new CreateMeasurementRequest(UUID.randomUUID(), new BigDecimal("-12.123456789123"),
                Instant.parse("2026-10-29T10:00:00.123456789Z"));
        var result = store.create(request);
        var captor = ArgumentCaptor.forClass(Measurement.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getTimestamp()).isEqualTo(Instant.parse("2026-10-29T10:00:00.123456Z"));
        assertThat(result.temperature()).isEqualByComparingTo(request.temperature());
        assertThat(result.evaluationStatus()).isEqualTo(EvaluationStatus.PENDING);
    }

    @Test
    void recordsEvaluationOnExistingMeasurement() {
        UUID id = UUID.randomUUID();
        var measurement = new Measurement(UUID.randomUUID(), BigDecimal.ZERO, Instant.now());
        when(repository.findById(id)).thenReturn(Optional.of(measurement));
        when(repository.saveAndFlush(measurement)).thenReturn(measurement);
        assertThat(store.recordEvaluation(id, EvaluationStatus.COMPLETED).evaluationStatus())
                .isEqualTo(EvaluationStatus.COMPLETED);
    }

    @Test
    void normalizesZeroExponentBeforePersistenceWithoutChangingValue() {
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = store.create(new CreateMeasurementRequest(UUID.randomUUID(),
                new BigDecimal("0e2147483647"), Instant.parse("2026-10-29T10:00:00Z")));
        assertThat(result.temperature()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void returnsEmptyListForPointWithoutMeasurements() {
        UUID point = UUID.randomUUID();
        when(repository.findByMonitoringPointIdOrderByTimestampAscIdAsc(point)).thenReturn(List.of());
        assertThat(store.list(point)).isEmpty();
    }
}
