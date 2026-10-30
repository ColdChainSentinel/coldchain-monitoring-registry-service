package pe.edu.upc.coldchain.alerts.service;

import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import pe.edu.upc.coldchain.alerts.client.RegistryClient;
import pe.edu.upc.coldchain.alerts.config.ThermalRulesProperties;
import pe.edu.upc.coldchain.alerts.dto.AlertResponse;
import pe.edu.upc.coldchain.alerts.dto.EvaluateMeasurementRequest;
import pe.edu.upc.coldchain.alerts.dto.EvaluationResponse;
import pe.edu.upc.coldchain.alerts.exception.MeasurementConflictException;
import pe.edu.upc.coldchain.alerts.exception.ThermalRuleNotConfiguredException;

@Service
public class EvaluationService {
    private final EvaluationStore store;
    private final RegistryClient registry;
    private final ThermalRulesProperties properties;
    private final ThermalEvaluator evaluator;

    public EvaluationService(EvaluationStore store, RegistryClient registry, ThermalRulesProperties properties,
            ThermalEvaluator evaluator) {
        this.store = store;
        this.registry = registry;
        this.properties = properties;
        this.evaluator = evaluator;
    }

    public EvaluationResponse evaluate(EvaluateMeasurementRequest input) {
        EvaluateMeasurementRequest request = input.normalized();
        var existing = store.find(request.measurementId());
        if (existing.isPresent()) {
            return originalResult(existing.get(), request);
        }
        // Registry I/O stays outside the local insertion transaction.
        registry.requirePoint(request.monitoringPointId());
        var rule = properties.rules().get(request.monitoringPointId());
        if (rule == null) {
            throw new ThermalRuleNotConfiguredException();
        }
        var type = evaluator.violation(request.temperature(), rule);
        try {
            return store.insert(request, rule, type);
        } catch (DataIntegrityViolationException exception) {
            // The losing INSERT has already rolled back; read the committed winner.
            var winner = store.find(request.measurementId());
            if (winner.isPresent()) {
                return originalResult(winner.get(), request);
            }
            throw exception;
        }
    }

    private EvaluationResponse originalResult(EvaluationStore.SavedEvaluation saved, EvaluateMeasurementRequest request) {
        if (!saved.matches(request)) {
            throw new MeasurementConflictException();
        }
        return saved.response();
    }

    public List<AlertResponse> list(UUID monitoringPointId) {
        return store.list(monitoringPointId);
    }
}
