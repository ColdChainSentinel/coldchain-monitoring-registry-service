package pe.edu.upc.coldchain.telemetry.service;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import pe.edu.upc.coldchain.telemetry.client.AlertsClient;
import pe.edu.upc.coldchain.telemetry.client.RegistryClient;
import pe.edu.upc.coldchain.telemetry.dto.CreateMeasurementRequest;
import pe.edu.upc.coldchain.telemetry.dto.MeasurementResponse;
import pe.edu.upc.coldchain.telemetry.model.EvaluationStatus;

@Service
public class MeasurementService {
    private static final Logger LOG = LoggerFactory.getLogger(MeasurementService.class);
    private final RegistryClient registry;
    private final AlertsClient alerts;
    private final MeasurementStore store;

    public MeasurementService(RegistryClient registry, AlertsClient alerts, MeasurementStore store) {
        this.registry = registry;
        this.alerts = alerts;
        this.store = store;
    }

    // HTTP calls intentionally stay outside DB transactions managed by MeasurementStore.
    public MeasurementResponse create(CreateMeasurementRequest request) {
        registry.requirePoint(request.monitoringPointId());
        MeasurementResponse persisted = store.create(request);
        EvaluationStatus status = alerts.evaluate(persisted);
        if (status == EvaluationStatus.PENDING) {
            return persisted;
        }
        try {
            return store.recordEvaluation(persisted.id(), status);
        } catch (DataAccessException | TransactionException exception) {
            LOG.warn("No se pudo actualizar evaluación; se conserva PENDING para measurementId={}", persisted.id());
            return persisted;
        }
    }

    public List<MeasurementResponse> list(UUID monitoringPointId) {
        return store.list(monitoringPointId);
    }
}
