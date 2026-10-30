package pe.edu.upc.coldchain.alerts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import pe.edu.upc.coldchain.alerts.client.RegistryClient;
import pe.edu.upc.coldchain.alerts.config.ThermalRule;
import pe.edu.upc.coldchain.alerts.config.ThermalRulesProperties;
import pe.edu.upc.coldchain.alerts.dto.EvaluateMeasurementRequest;
import pe.edu.upc.coldchain.alerts.dto.EvaluationResponse;
import pe.edu.upc.coldchain.alerts.exception.MeasurementConflictException;
import pe.edu.upc.coldchain.alerts.exception.MonitoringPointNotFoundException;
import pe.edu.upc.coldchain.alerts.exception.RegistryUnavailableException;
import pe.edu.upc.coldchain.alerts.exception.ThermalRuleNotConfiguredException;
import pe.edu.upc.coldchain.alerts.model.AlertType;
import pe.edu.upc.coldchain.alerts.model.EvaluationOutcome;
import pe.edu.upc.coldchain.alerts.service.EvaluationService;
import pe.edu.upc.coldchain.alerts.service.EvaluationStore;
import pe.edu.upc.coldchain.alerts.service.ThermalEvaluator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {
    private final UUID point = UUID.randomUUID();
    private final EvaluateMeasurementRequest request = new EvaluateMeasurementRequest(UUID.randomUUID(), point,
            new BigDecimal("9"), Instant.parse("2026-10-29T10:00:00Z"));
    private final ThermalRule rule = new ThermalRule(new BigDecimal("2"), new BigDecimal("8"));
    private final EvaluationResponse original = new EvaluationResponse(request.measurementId(), EvaluationOutcome.NO_ALERT, null);
    @Mock
    private EvaluationStore store;
    @Mock
    private RegistryClient registry;
    private EvaluationService service;

    @BeforeEach
    void setUp() {
        service = new EvaluationService(store, registry, new ThermalRulesProperties(Map.of(point, rule)), new ThermalEvaluator());
    }

    private EvaluationStore.SavedEvaluation saved(EvaluateMeasurementRequest input) {
        return new EvaluationStore.SavedEvaluation(input.monitoringPointId(), input.temperature(), input.timestamp(), original);
    }

    @Test
    void validatesPointBeforeAtomicInsertion() {
        when(store.insert(request, rule, AlertType.HIGH_TEMPERATURE)).thenReturn(original);
        assertThat(service.evaluate(request)).isEqualTo(original);
        var order = inOrder(store, registry);
        order.verify(store).find(request.measurementId());
        order.verify(registry).requirePoint(point);
        order.verify(store).insert(request, rule, AlertType.HIGH_TEMPERATURE);
    }

    @Test
    void duplicateReturnsOriginalWithoutRegistryOrRuleLookup() {
        service = new EvaluationService(store, registry, new ThermalRulesProperties(Map.of()), new ThermalEvaluator());
        when(store.find(request.measurementId())).thenReturn(Optional.of(saved(request)));
        assertThat(service.evaluate(request)).isEqualTo(original);
        verifyNoInteractions(registry);
        verify(store, never()).insert(any(), any(), any());
    }

    @Test
    void changedDataForExistingIdConflictsEvenWithoutRegistry() {
        var changed = new EvaluateMeasurementRequest(request.measurementId(), point, BigDecimal.ZERO, request.timestamp());
        when(store.find(request.measurementId())).thenReturn(Optional.of(saved(request)));
        assertThatThrownBy(() -> service.evaluate(changed)).isInstanceOf(MeasurementConflictException.class);
        verifyNoInteractions(registry);
    }

    @Test
    void replayIgnoresDecimalScaleAndNormalizesTimestamp() {
        when(store.find(request.measurementId())).thenReturn(Optional.of(saved(request)));
        var equivalent = new EvaluateMeasurementRequest(request.measurementId(), point, new BigDecimal("9.0000"),
                request.timestamp().plusNanos(123));
        assertThat(service.evaluate(equivalent)).isEqualTo(original);
        verifyNoInteractions(registry);
    }

    @Test
    void missingPointDoesNotInsert() {
        doThrow(new MonitoringPointNotFoundException()).when(registry).requirePoint(point);
        assertThatThrownBy(() -> service.evaluate(request)).isInstanceOf(MonitoringPointNotFoundException.class);
        verify(store, never()).insert(any(), any(), any());
    }

    @Test
    void registryFailureDoesNotInsert() {
        doThrow(new RegistryUnavailableException()).when(registry).requirePoint(point);
        assertThatThrownBy(() -> service.evaluate(request)).isInstanceOf(RegistryUnavailableException.class);
        verify(store, never()).insert(any(), any(), any());
    }

    @Test
    void pointWithoutRuleIsNotEvaluatedWithDefaults() {
        service = new EvaluationService(store, registry, new ThermalRulesProperties(Map.of()), new ThermalEvaluator());
        assertThatThrownBy(() -> service.evaluate(request)).isInstanceOf(ThermalRuleNotConfiguredException.class);
        verify(store, never()).insert(any(), any(), any());
    }

    @Test
    void concurrentDuplicateReturnsCommittedWinnerAfterRollback() {
        when(store.find(request.measurementId())).thenReturn(Optional.empty()).thenReturn(Optional.of(saved(request)));
        when(store.insert(request, rule, AlertType.HIGH_TEMPERATURE)).thenThrow(new DataIntegrityViolationException("collision"));
        assertThat(service.evaluate(request)).isEqualTo(original);
    }

    @Test
    void concurrentConflictingDuplicateReturns409() {
        var other = new EvaluateMeasurementRequest(request.measurementId(), point, BigDecimal.ONE, request.timestamp());
        when(store.find(request.measurementId())).thenReturn(Optional.empty()).thenReturn(Optional.of(saved(other)));
        when(store.insert(request, rule, AlertType.HIGH_TEMPERATURE)).thenThrow(new DataIntegrityViolationException("collision"));
        assertThatThrownBy(() -> service.evaluate(request)).isInstanceOf(MeasurementConflictException.class);
    }

    @Test
    void unrelatedIntegrityFailureIsNotMistakenForSuccessfulReplay() {
        var failure = new DataIntegrityViolationException("other constraint");
        when(store.insert(request, rule, AlertType.HIGH_TEMPERATURE)).thenThrow(failure);
        assertThatThrownBy(() -> service.evaluate(request)).isSameAs(failure);
    }

    @Test
    void queryingAlertsNeedsNoExternalService() {
        when(store.list(point)).thenReturn(List.of());
        assertThat(service.list(point)).isEmpty();
        verifyNoInteractions(registry);
    }
}
