package pe.edu.upc.coldchain.integration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntegratedSteps {
    private static final ServiceStack STACK = new ServiceStack();
    private String point;
    private int initialMeasurements;
    private ServiceStack.Reply telemetryReply;
    private ServiceStack.Reply alertsReply;
    private JsonNode measurement;
    private JsonNode firstEvaluation;
    private JsonNode secondEvaluation;

    @BeforeAll
    public static void startStack() throws Exception {
        STACK.start();
    }

    @AfterAll
    public static void stopStack() {
        STACK.close();
    }

    @After
    public void restoreAvailability() throws Exception {
        STACK.startAlerts();
    }

    @Dado("un punto de monitoreo con regla térmica de 2 a 8 grados Celsius")
    public void configuredPoint() throws Exception {
        selectPoint(STACK.point);
    }

    @Dado("un punto existente sin regla térmica")
    public void unconfiguredPoint() throws Exception {
        selectPoint(STACK.noRulePoint);
    }

    @Dado("un identificador de punto que no existe en Registry")
    public void nonexistentPoint() throws Exception {
        selectPoint(UUID.randomUUID().toString());
    }

    private void selectPoint(String id) throws Exception {
        point = id;
        initialMeasurements = measurements().size();
    }

    @Cuando("envío la siguiente medición para el punto:")
    public void postTable(DataTable table) throws Exception {
        var rows = table.asMaps(String.class, String.class);
        assertEquals(1, rows.size());
        assertEquals(java.util.Set.of("temperature", "timestamp"), rows.get(0).keySet());
        sendMeasurement(new BigDecimal(rows.get(0).get("temperature")), rows.get(0).get("timestamp"));
    }

    @Cuando("registro una medición de {bigdecimal} grados Celsius")
    public void postTemperature(BigDecimal temperature) throws Exception {
        sendMeasurement(temperature, "2026-10-01T12:00:00Z");
    }

    private void sendMeasurement(BigDecimal temperature, String timestamp) throws Exception {
        telemetryReply = STACK.telemetry("POST", "/api/measurements",
                Map.of("monitoringPointId", point, "temperature", temperature, "timestamp", timestamp));
        if (telemetryReply.status() == 201) {
            measurement = telemetryReply.body();
            assertEquals(point, measurement.path("monitoringPointId").asText());
            assertEquals(0, temperature.compareTo(measurement.path("temperature").decimalValue()));
            assertEquals(Instant.parse(timestamp), Instant.parse(measurement.path("timestamp").asText()));
            UUID.fromString(measurement.path("id").asText());
        }
    }

    @Cuando("envío una medición sin datos")
    public void postEmpty() throws Exception {
        telemetryReply = STACK.telemetry("POST", "/api/measurements", Map.of());
    }

    @Entonces("Telemetry responde con estado {int}")
    public void telemetryStatus(int expected) {
        assertNotNull(telemetryReply);
        assertEquals(expected, telemetryReply.status(), telemetryReply.body().toString());
    }

    @Entonces("Alerts responde con estado {int}")
    public void alertsStatus(int expected) {
        assertNotNull(alertsReply);
        assertEquals(expected, alertsReply.status(), alertsReply.body().toString());
    }

    @Entonces("la medición queda consultable con temperatura {bigdecimal} y fecha UTC {string}")
    public void queryMeasurement(BigDecimal temperature, String timestamp) throws Exception {
        JsonNode saved = storedMeasurement();
        assertEquals(0, temperature.compareTo(saved.path("temperature").decimalValue()));
        assertEquals(timestamp, saved.path("timestamp").asText());
        onlyOneMeasurementAdded();
    }

    @Entonces("su evaluación queda en estado {string}")
    public void evaluationStatus(String expected) throws Exception {
        assertNotNull(measurement, "La solicitud debe haber creado una medición");
        assertEquals(expected, measurement.path("evaluationStatus").asText());
        queryStatus(expected);
    }

    @Entonces("la consulta de Telemetry conserva el estado {string}")
    public void queryStatus(String expected) throws Exception {
        assertEquals(expected, storedMeasurement().path("evaluationStatus").asText());
    }

    @Entonces("no hay alertas asociadas a la medición")
    public void noAlerts() throws Exception {
        alertCount(0, "NINGUNA");
    }

    @Entonces("existen {int} alertas de tipo {string} asociadas a la medición")
    public void alertCount(int expected, String type) throws Exception {
        assertNotNull(measurement);
        var response = STACK.alerts("GET", "/api/alerts?monitoringPointId=" + point, null);
        assertEquals(200, response.status());
        assertTrue(response.body().isArray());
        List<JsonNode> matches = new ArrayList<>();
        for (JsonNode alert : response.body()) {
            if (alert.path("measurementId").asText().equals(measurement.path("id").asText())) {
                matches.add(alert);
            }
        }
        assertEquals(expected, matches.size());
        for (JsonNode alert : matches) {
            UUID.fromString(alert.path("id").asText());
            assertEquals(point, alert.path("monitoringPointId").asText());
            assertEquals(0, measurement.path("temperature").decimalValue().compareTo(alert.path("temperature").decimalValue()));
            assertEquals(0, new BigDecimal("2").compareTo(alert.path("thresholdMin").decimalValue()));
            assertEquals(0, new BigDecimal("8").compareTo(alert.path("thresholdMax").decimalValue()));
            assertEquals("OPEN", alert.path("status").asText());
            assertEquals(type, alert.path("type").asText());
            Instant.parse(alert.path("createdAt").asText());
        }
    }

    @Entonces("se informan los campos obligatorios de la medición:")
    public void requiredFields(DataTable table) {
        for (var row : table.asMaps(String.class, String.class)) {
            assertFalse(telemetryReply.body().path("fields").path(row.get("campo")).asText().isBlank());
        }
    }

    @Entonces("no se añade una medición al punto")
    public void noMeasurementAdded() throws Exception {
        assertEquals(initialMeasurements, measurements().size());
    }

    @Entonces("el punto inexistente no tiene mediciones")
    public void unknownPointIsEmpty() throws Exception {
        assertTrue(measurements().isEmpty());
    }

    @Entonces("solo se añadió una medición al punto")
    public void onlyOneMeasurementAdded() throws Exception {
        assertEquals(initialMeasurements + 1, measurements().size());
    }

    @Cuando("repito dos veces la evaluación con los mismos datos")
    public void replay() throws Exception {
        firstEvaluation = evaluate(evaluationInput());
        secondEvaluation = evaluate(evaluationInput());
    }

    @Entonces("ambas evaluaciones devuelven el mismo resultado y la misma alerta")
    public void sameEvaluation() {
        assertEquals(firstEvaluation, secondEvaluation);
        assertEquals("ALERT_CREATED", firstEvaluation.path("outcome").asText());
        assertEquals(measurement.path("id").asText(), firstEvaluation.path("measurementId").asText());
        UUID.fromString(firstEvaluation.path("alert").path("id").asText());
    }

    @Cuando("intento evaluar la misma medición con temperatura {bigdecimal}")
    public void conflict(BigDecimal temperature) throws Exception {
        ObjectNode changed = evaluationInput();
        changed.put("temperature", temperature);
        alertsReply = STACK.alerts("POST", "/api/alerts/evaluate", changed);
    }

    @Dado("Alerts deja de estar disponible")
    public void stopAlerts() {
        STACK.stopAlerts();
    }

    @Cuando("Alerts vuelve a estar disponible y reevalúo la medición guardada")
    public void recover() throws Exception {
        STACK.startAlerts();
        JsonNode result = evaluate(evaluationInput());
        assertEquals("ALERT_CREATED", result.path("outcome").asText());
    }

    private ObjectNode evaluationInput() {
        assertNotNull(measurement);
        ObjectNode input = ServiceStack.JSON.createObjectNode();
        input.set("measurementId", measurement.path("id"));
        input.set("monitoringPointId", measurement.path("monitoringPointId"));
        input.set("temperature", measurement.path("temperature"));
        input.set("timestamp", measurement.path("timestamp"));
        return input;
    }

    private JsonNode evaluate(ObjectNode input) throws Exception {
        var reply = STACK.alerts("POST", "/api/alerts/evaluate", input);
        assertEquals(200, reply.status(), reply.body().toString());
        return reply.body();
    }

    private JsonNode measurements() throws Exception {
        var reply = STACK.telemetry("GET", "/api/measurements?monitoringPointId=" + point, null);
        assertEquals(200, reply.status());
        assertTrue(reply.body().isArray());
        return reply.body();
    }

    private JsonNode storedMeasurement() throws Exception {
        assertNotNull(measurement);
        for (JsonNode saved : measurements()) {
            if (saved.path("id").asText().equals(measurement.path("id").asText())) {
                return saved;
            }
        }
        throw new AssertionError("La medición creada no aparece en la consulta");
    }
}
