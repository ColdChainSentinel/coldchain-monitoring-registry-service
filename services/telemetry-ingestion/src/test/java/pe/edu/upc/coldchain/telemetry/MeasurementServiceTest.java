package pe.edu.upc.coldchain.telemetry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import pe.edu.upc.coldchain.telemetry.client.AlertsClient;
import pe.edu.upc.coldchain.telemetry.client.RegistryClient;
import pe.edu.upc.coldchain.telemetry.dto.CreateMeasurementRequest;
import pe.edu.upc.coldchain.telemetry.dto.MeasurementResponse;
import pe.edu.upc.coldchain.telemetry.exception.MonitoringPointNotFoundException;
import pe.edu.upc.coldchain.telemetry.exception.RegistryUnavailableException;
import pe.edu.upc.coldchain.telemetry.model.EvaluationStatus;
import pe.edu.upc.coldchain.telemetry.service.MeasurementService;
import pe.edu.upc.coldchain.telemetry.service.MeasurementStore;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeasurementServiceTest {
    private final UUID point = UUID.randomUUID();
    private final CreateMeasurementRequest request = new CreateMeasurementRequest(point,
            new BigDecimal("-3.125"), Instant.parse("2026-10-29T10:00:00Z"));
    private final MeasurementResponse pending = new MeasurementResponse(UUID.randomUUID(), point,
            request.temperature(), request.timestamp(), EvaluationStatus.PENDING);
    @Mock
    private RegistryClient registry;
    @Mock
    private AlertsClient alerts;
    @Mock
    private MeasurementStore store;
    @InjectMocks
    private MeasurementService service;

    @Test
    void validatesThenCommitsBeforeCallingAlertsAndUpdatingStatus() {
        var completed = new MeasurementResponse(pending.id(), point, request.temperature(),
                request.timestamp(), EvaluationStatus.COMPLETED);
        when(store.create(request)).thenReturn(pending);
        when(alerts.evaluate(pending)).thenReturn(EvaluationStatus.COMPLETED);
        when(store.recordEvaluation(pending.id(), EvaluationStatus.COMPLETED)).thenReturn(completed);

        assertThat(service.create(request)).isEqualTo(completed);
        var order = inOrder(registry, store, alerts);
        order.verify(registry).requirePoint(point);
        order.verify(store).create(request);
        order.verify(alerts).evaluate(pending);
        order.verify(store).recordEvaluation(pending.id(), EvaluationStatus.COMPLETED);
    }

    @Test
    void missingPointDoesNotStoreOrEvaluate() {
        doThrow(new MonitoringPointNotFoundException()).when(registry).requirePoint(point);
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(MonitoringPointNotFoundException.class);
        verifyNoInteractions(store, alerts);
    }

    @Test
    void registryUnavailableDoesNotStoreOrEvaluate() {
        doThrow(new RegistryUnavailableException()).when(registry).requirePoint(point);
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(RegistryUnavailableException.class);
        verifyNoInteractions(store, alerts);
    }

    @Test
    void storageFailureDoesNotEvaluate() {
        when(store.create(request)).thenThrow(new DataAccessResourceFailureException("test"));
        assertThatThrownBy(() -> service.create(request)).isInstanceOf(DataAccessResourceFailureException.class);
        verifyNoInteractions(alerts);
    }

    @Test
    void failedEvaluationKeepsPersistedMeasurementPending() {
        when(store.create(request)).thenReturn(pending);
        when(alerts.evaluate(pending)).thenReturn(EvaluationStatus.PENDING);
        assertThat(service.create(request)).isEqualTo(pending);
        verify(store, never()).recordEvaluation(pending.id(), EvaluationStatus.PENDING);
    }

    @Test
    void missingRuleIsPersistedAsSuch() {
        var result = new MeasurementResponse(pending.id(), point, request.temperature(),
                request.timestamp(), EvaluationStatus.RULE_NOT_CONFIGURED);
        when(store.create(request)).thenReturn(pending);
        when(alerts.evaluate(pending)).thenReturn(EvaluationStatus.RULE_NOT_CONFIGURED);
        when(store.recordEvaluation(pending.id(), EvaluationStatus.RULE_NOT_CONFIGURED)).thenReturn(result);
        assertThat(service.create(request)).isEqualTo(result);
    }

    @Test
    void statusUpdateFailureDoesNotDenyAlreadyCommittedMeasurement() {
        when(store.create(request)).thenReturn(pending);
        when(alerts.evaluate(pending)).thenReturn(EvaluationStatus.COMPLETED);
        when(store.recordEvaluation(pending.id(), EvaluationStatus.COMPLETED))
                .thenThrow(new DataAccessResourceFailureException("test"));
        assertThat(service.create(request)).isEqualTo(pending);
    }

    @Test
    void readsWithoutCallingExternalServices() {
        when(store.list(point)).thenReturn(List.of(pending));
        assertThat(service.list(point)).containsExactly(pending);
        verifyNoInteractions(registry, alerts);
    }
}
