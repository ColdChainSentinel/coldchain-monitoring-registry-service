package pe.edu.upc.coldchain.alerts.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.coldchain.alerts.config.ThermalRule;
import pe.edu.upc.coldchain.alerts.dto.AlertResponse;
import pe.edu.upc.coldchain.alerts.dto.EvaluateMeasurementRequest;
import pe.edu.upc.coldchain.alerts.dto.EvaluationResponse;
import pe.edu.upc.coldchain.alerts.model.Alert;
import pe.edu.upc.coldchain.alerts.model.AlertType;
import pe.edu.upc.coldchain.alerts.model.MeasurementEvaluation;
import pe.edu.upc.coldchain.alerts.repository.AlertRepository;
import pe.edu.upc.coldchain.alerts.repository.EvaluationRepository;

@Repository
@Transactional(readOnly = true)
public class EvaluationStore {
    @PersistenceContext
    private EntityManager entityManager;
    private final EvaluationRepository evaluations;
    private final AlertRepository alerts;
    private final Clock clock;

    public EvaluationStore(EvaluationRepository evaluations, AlertRepository alerts, Clock clock) {
        this.evaluations = evaluations;
        this.alerts = alerts;
        this.clock = clock;
    }

    public Optional<SavedEvaluation> find(UUID measurementId) {
        return evaluations.findById(measurementId).map(evaluation -> new SavedEvaluation(
                evaluation.getMonitoringPointId(), evaluation.getTemperature(), evaluation.getTimestamp(),
                EvaluationResponse.from(evaluation)));
    }

    @Transactional
    public EvaluationResponse insert(EvaluateMeasurementRequest request, ThermalRule rule, AlertType type) {
        Alert alert = type == null ? null : new Alert(request.monitoringPointId(), request.measurementId(),
                request.temperature(), rule.thresholdMin().stripTrailingZeros(), rule.thresholdMax().stripTrailingZeros(),
                type, Instant.now(clock).truncatedTo(ChronoUnit.MICROS));
        MeasurementEvaluation evaluation = new MeasurementEvaluation(request.measurementId(), request.monitoringPointId(),
                request.temperature(), request.timestamp(), alert);
        // Assigned measurement IDs must INSERT, never merge into an existing evaluation.
        entityManager.persist(evaluation);
        entityManager.flush();
        return EvaluationResponse.from(evaluation);
    }

    public List<AlertResponse> list(UUID pointId) {
        return alerts.findByMonitoringPointIdOrderByCreatedAtAscIdAsc(pointId)
                .stream().map(AlertResponse::from).toList();
    }

    public record SavedEvaluation(UUID monitoringPointId, BigDecimal temperature,
            Instant timestamp, EvaluationResponse response) {
        public boolean matches(EvaluateMeasurementRequest request) {
            return monitoringPointId.equals(request.monitoringPointId())
                    && temperature.compareTo(request.temperature()) == 0 && timestamp.equals(request.timestamp());
        }
    }
}
