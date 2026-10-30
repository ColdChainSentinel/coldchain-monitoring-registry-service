package pe.edu.upc.coldchain.telemetry.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pe.edu.upc.coldchain.telemetry.dto.MeasurementResponse;
import pe.edu.upc.coldchain.telemetry.model.EvaluationStatus;

@Component
public class AlertsClient {
    private static final Logger LOG = LoggerFactory.getLogger(AlertsClient.class);
    private final RestClient http;

    public AlertsClient(@Qualifier("alertsRestClient") RestClient http) {
        this.http = http;
    }

    public EvaluationStatus evaluate(MeasurementResponse measurement) {
        try {
            var response = http.post().uri("/api/alerts/evaluate").contentType(MediaType.APPLICATION_JSON)
                    .body(new EvaluationRequest(measurement.id(), measurement.monitoringPointId(),
                            measurement.temperature(), measurement.timestamp()))
                    .retrieve().toEntity(EvaluationResult.class);
            EvaluationResult result = response.getBody();
            if (response.getStatusCode().value() == 200 && result != null
                    && measurement.id().equals(result.measurementId())) {
                if ("NO_ALERT".equals(result.outcome()) && result.alert() == null) {
                    return EvaluationStatus.COMPLETED;
                }
                if ("ALERT_CREATED".equals(result.outcome()) && result.alert() != null
                        && result.alert().matches(measurement)) {
                    return EvaluationStatus.COMPLETED;
                }
            }
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode().value() == 422) {
                try {
                    RuleProblem problem = exception.getResponseBodyAs(RuleProblem.class);
                    if (problem != null && "THERMAL_RULE_NOT_CONFIGURED".equals(problem.code())) {
                        return EvaluationStatus.RULE_NOT_CONFIGURED;
                    }
                } catch (RestClientException | IllegalStateException ignored) {
                    // A malformed error is a failed evaluation, never a successful one.
                }
            }
        } catch (RestClientException exception) {
            // Persisted measurements remain usable during downstream failures.
        }
        LOG.warn("Evaluación pendiente para measurementId={}", measurement.id());
        return EvaluationStatus.PENDING;
    }

    public record EvaluationRequest(UUID measurementId, UUID monitoringPointId, BigDecimal temperature,
            Instant timestamp) { }
    public record EvaluationResult(UUID measurementId, String outcome, AlertResult alert) { }
    public record RuleProblem(String code) { }
    public record AlertResult(UUID id, UUID monitoringPointId, UUID measurementId, BigDecimal temperature,
            BigDecimal thresholdMin, BigDecimal thresholdMax, String status, Instant createdAt) {
        boolean matches(MeasurementResponse measurement) {
            return id != null && measurement.monitoringPointId().equals(monitoringPointId)
                    && measurement.id().equals(measurementId) && temperature != null
                    && temperature.compareTo(measurement.temperature()) == 0
                    && thresholdMin != null && thresholdMax != null && thresholdMin.compareTo(thresholdMax) <= 0
                    && "OPEN".equals(status) && createdAt != null;
        }
    }
}
