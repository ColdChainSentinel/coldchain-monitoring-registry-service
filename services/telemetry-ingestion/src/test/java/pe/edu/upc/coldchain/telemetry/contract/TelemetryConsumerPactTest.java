package pe.edu.upc.coldchain.telemetry.contract;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.SerializationFeature;
import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.annotations.PactDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;
import pe.edu.upc.coldchain.telemetry.client.AlertsClient;
import pe.edu.upc.coldchain.telemetry.client.RegistryClient;
import pe.edu.upc.coldchain.telemetry.dto.MeasurementResponse;
import pe.edu.upc.coldchain.telemetry.exception.MonitoringPointNotFoundException;
import pe.edu.upc.coldchain.telemetry.model.EvaluationStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de contrato (consumer-driven) de Telemetry Ingestion con sus dos
 * proveedores. Cada prueba levanta un mock server de Pact, ejecuta el cliente real
 * y, si pasa, publica el contrato en contracts/pacts. Monitoring Registry y Alert
 * Management lo verifican contra su API real (ver *ProviderPactTest).
 */
@ExtendWith(PactConsumerTestExt.class)
@PactDirectory("../../contracts/pacts")
class TelemetryConsumerPactTest {
    private static final String POINT = "5a9e6715-2054-4c44-920c-440efd3bafb2";
    private static final String MEASUREMENT = "c461669a-8f6f-47b4-ba90-4927de891993";
    private static final String ISO = "yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]'Z'";

    /** Mismo formato JSON que la aplicación (fechas ISO-8601, no epoch). */
    private static RestClient http(MockServer server) {
        var json = new MappingJackson2HttpMessageConverter(Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build());
        return RestClient.builder().baseUrl(server.getUrl())
                .messageConverters(converters -> converters.add(0, json)).build();
    }

    // ---------------------------------------------------------------- Registry

    @Pact(consumer = "telemetry-ingestion", provider = "monitoring-registry")
    V4Pact existingPoint(PactDslWithProvider builder) {
        return builder
                .given("existe un punto de monitoreo")
                .uponReceiving("consulta de un punto existente")
                .method("GET")
                .pathFromProviderState("/api/v1/monitoring-points/${pointId}", "/api/v1/monitoring-points/" + POINT)
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody().valueFromProviderState("id", "${pointId}", POINT))
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "telemetry-ingestion", provider = "monitoring-registry")
    V4Pact missingPoint(PactDslWithProvider builder) {
        return builder
                .given("no existe el punto de monitoreo")
                .uponReceiving("consulta de un punto inexistente")
                .method("GET")
                .path("/api/v1/monitoring-points/00000000-0000-4000-8000-000000000000")
                .willRespondWith()
                .status(404)
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "existingPoint")
    void acceptsAnExistingPoint(MockServer server) {
        var client = new RegistryClient(http(server));
        assertThatNoException().isThrownBy(() -> client.requirePoint(UUID.fromString(POINT)));
    }

    @Test
    @PactTestFor(pactMethod = "missingPoint")
    void rejectsAMissingPoint(MockServer server) {
        var client = new RegistryClient(http(server));
        assertThatThrownBy(() -> client.requirePoint(UUID.fromString("00000000-0000-4000-8000-000000000000")))
                .isInstanceOf(MonitoringPointNotFoundException.class);
    }

    // ------------------------------------------------------------------ Alerts

    private static PactDslJsonBody evaluation(String temperature) {
        return (PactDslJsonBody) new PactDslJsonBody()
                .stringValue("measurementId", MEASUREMENT)
                .valueFromProviderState("monitoringPointId", "${pointId}", POINT)
                .numberValue("temperature", new BigDecimal(temperature))
                .stringValue("timestamp", "2026-11-26T10:00:00Z");
    }

    @Pact(consumer = "telemetry-ingestion", provider = "alert-management")
    V4Pact outOfRange(PactDslWithProvider builder) {
        return builder
                .given("el punto tiene un rango térmico de 2 a 8 °C")
                .uponReceiving("evaluación de una medición fuera de rango")
                .method("POST")
                .path("/api/alerts/evaluate")
                .headers(Map.of("Content-Type", "application/json"))
                .body(evaluation("10.5"))
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .stringValue("measurementId", MEASUREMENT)
                        .stringValue("outcome", "ALERT_CREATED")
                        .object("alert")
                            .uuid("id")
                            .valueFromProviderState("monitoringPointId", "${pointId}", POINT)
                            .stringValue("measurementId", MEASUREMENT)
                            .decimalType("temperature", 10.5)
                            .numberType("thresholdMin", 2)
                            .numberType("thresholdMax", 8)
                            .stringValue("status", "OPEN")
                            .stringMatcher("createdAt", "\\d{4}-\\d{2}-\\d{2}T.+Z", "2026-11-26T10:00:01Z")
                        .closeObject())
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "telemetry-ingestion", provider = "alert-management")
    V4Pact withinRange(PactDslWithProvider builder) {
        return builder
                .given("el punto tiene un rango térmico de 2 a 8 °C")
                .uponReceiving("evaluación de una medición dentro del rango")
                .method("POST")
                .path("/api/alerts/evaluate")
                .headers(Map.of("Content-Type", "application/json"))
                .body(evaluation("5"))
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .stringValue("measurementId", MEASUREMENT)
                        .stringValue("outcome", "NO_ALERT"))
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "telemetry-ingestion", provider = "alert-management")
    V4Pact noRule(PactDslWithProvider builder) {
        return builder
                .given("el punto no tiene rango térmico")
                .uponReceiving("evaluación de una medición sin regla configurada")
                .method("POST")
                .path("/api/alerts/evaluate")
                .headers(Map.of("Content-Type", "application/json"))
                .body(evaluation("5"))
                .willRespondWith()
                .status(422)
                .headers(Map.of("Content-Type", "application/problem+json"))
                .body(new PactDslJsonBody().stringValue("code", "THERMAL_RULE_NOT_CONFIGURED"))
                .toPact(V4Pact.class);
    }

    private static MeasurementResponse measurement(String temperature) {
        return new MeasurementResponse(UUID.fromString(MEASUREMENT), UUID.fromString(POINT),
                new BigDecimal(temperature), Instant.parse("2026-11-26T10:00:00Z"), EvaluationStatus.PENDING);
    }

    private static AlertsClient alerts(MockServer server) {
        return new AlertsClient(http(server));
    }

    @Test
    @PactTestFor(pactMethod = "outOfRange")
    void outOfRangeMeasurementCompletesWithAlert(MockServer server) {
        assertThat(alerts(server).evaluate(measurement("10.5"))).isEqualTo(EvaluationStatus.COMPLETED);
    }

    @Test
    @PactTestFor(pactMethod = "withinRange")
    void inRangeMeasurementCompletesWithoutAlert(MockServer server) {
        assertThat(alerts(server).evaluate(measurement("5"))).isEqualTo(EvaluationStatus.COMPLETED);
    }

    @Test
    @PactTestFor(pactMethod = "noRule")
    void missingRuleIsReportedAsRuleNotConfigured(MockServer server) {
        assertThat(alerts(server).evaluate(measurement("5"))).isEqualTo(EvaluationStatus.RULE_NOT_CONFIGURED);
    }
}
