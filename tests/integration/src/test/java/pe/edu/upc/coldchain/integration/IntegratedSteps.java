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
import io.cucumber.java.Before;
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
    private int initialAlerts;
    private BigDecimal expectedMin = new BigDecimal("2");
    private BigDecimal expectedMax = new BigDecimal("8");
    private JsonNode previousMeasurement;
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

    @Before("@requiere_postgres")
    public void requirePersistentDatabase() {
        // H2 loses data when a process restarts; these scenarios must be excluded, never passed in H2.
        assertEquals("postgres", STACK.mode(), "Escenario @requiere_postgres ejecutado sin PostgreSQL");
    }

    @After
    public void restoreAvailability() throws Exception {
        STACK.startRegistry();
        STACK.startAlerts();
        STACK.restoreRules();
    }

    @Dado("un punto de monitoreo con regla térmica de 2 a 8 grados Celsius")
    public void configuredPoint() throws Exception {
        selectPoint(STACK.point);
    }

    @Dado("un punto de monitoreo con regla térmica propia de 2 a 8 grados Celsius")
    public void versionedPoint() throws Exception {
        selectPoint(STACK.versionPoint);
    }

    @Dado("un punto nuevo sin mediciones")
    public void newPoint() throws Exception {
        selectPoint(STACK.createPoint(newCode()));
        assertEquals(0, initialMeasurements);
    }

    @Dado("otro punto nuevo con una medición de {bigdecimal} grados Celsius")
    public void otherPointWithMeasurement(BigDecimal temperature) throws Exception {
        String other = STACK.createPoint(newCode());
        var reply = STACK.telemetry("POST", "/api/measurements",
                Map.of("monitoringPointId", other, "temperature", temperature, "timestamp", "2026-10-01T12:05:00Z"));
        assertEquals(201, reply.status(), reply.body().toString());
    }

    private static String newCode() {
        return "IT-H-" + UUID.randomUUID().toString().substring(0, 8);
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
        initialAlerts = alerts().size();
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
        previousMeasurement = measurement;
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

    @Cuando("registro las siguientes mediciones en este orden:")
    public void postSequence(DataTable table) throws Exception {
        for (var row : table.asMaps(String.class, String.class)) {
            sendMeasurement(new BigDecimal(row.get("temperature")), row.get("timestamp"));
            telemetryStatus(201);
        }
    }

    @Cuando("envío una medición para el punto con temperatura {} y fecha {}")
    public void postRaw(String temperature, String timestamp) throws Exception {
        // Literal JSON values: rejected types must reach Telemetry exactly as written.
        JsonNode body = ServiceStack.JSON.readTree("{\"monitoringPointId\":\"" + point + "\",\"temperature\":"
                + temperature + ",\"timestamp\":" + timestamp + "}");
        telemetryReply = STACK.telemetry("POST", "/api/measurements", body);
    }

    @Cuando("envío una medición con el identificador de punto {string}")
    public void postWithPointId(String id) throws Exception {
        telemetryReply = STACK.telemetry("POST", "/api/measurements",
                Map.of("monitoringPointId", id, "temperature", 5, "timestamp", "2026-10-01T12:00:00Z"));
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
        List<JsonNode> matches = new ArrayList<>();
        for (JsonNode alert : alerts()) {
            if (alert.path("measurementId").asText().equals(measurement.path("id").asText())) {
                matches.add(alert);
            }
        }
        assertEquals(expected, matches.size());
        for (JsonNode alert : matches) {
            UUID.fromString(alert.path("id").asText());
            assertEquals(point, alert.path("monitoringPointId").asText());
            assertEquals(0, measurement.path("temperature").decimalValue().compareTo(alert.path("temperature").decimalValue()));
            assertLimits(alert, expectedMin, expectedMax);
            assertEquals("OPEN", alert.path("status").asText());
            assertEquals(type, alert.path("type").asText());
            Instant.parse(alert.path("createdAt").asText());
        }
    }

    @Entonces("se informa el motivo del rechazo")
    public void rejectionReason() {
        assertFalse(telemetryReply.body().path("detail").asText().isBlank(), telemetryReply.body().toString());
    }

    @Entonces("no se generan alertas nuevas para el punto")
    public void noNewAlerts() throws Exception {
        assertEquals(initialAlerts, alerts().size());
    }

    @Entonces("el historial del punto contiene {int} mediciones ordenadas por fecha de medición:")
    public void history(int expected, DataTable table) throws Exception {
        JsonNode saved = measurements();
        var rows = table.asMaps(String.class, String.class);
        assertEquals(expected, rows.size());
        assertEquals(expected, saved.size());
        for (int i = 0; i < rows.size(); i++) {
            assertEquals(point, saved.get(i).path("monitoringPointId").asText());
            assertEquals(0, new BigDecimal(rows.get(i).get("temperature")).compareTo(saved.get(i).path("temperature").decimalValue()));
            assertEquals(rows.get(i).get("timestamp"), saved.get(i).path("timestamp").asText());
        }
    }

    @Entonces("la medición más reciente del punto tiene temperatura {bigdecimal}")
    public void latest(BigDecimal temperature) throws Exception {
        JsonNode saved = measurements();
        assertFalse(saved.isEmpty());
        assertEquals(0, temperature.compareTo(saved.get(saved.size() - 1).path("temperature").decimalValue()));
    }

    @Entonces("el historial del punto está vacío")
    public void emptyHistory() throws Exception {
        assertTrue(measurements().isEmpty());
    }

    @Cuando("solicito a Alerts evaluar una medición nueva de {bigdecimal} grados Celsius")
    public void evaluateDirectly(BigDecimal temperature) throws Exception {
        alertsReply = STACK.alerts("POST", "/api/alerts/evaluate", Map.of("measurementId", UUID.randomUUID().toString(),
                "monitoringPointId", point, "temperature", temperature, "timestamp", "2026-10-01T12:00:00Z"));
    }

    @Cuando("solicito a Alerts evaluar una petición sin datos")
    public void evaluateEmpty() throws Exception {
        alertsReply = STACK.alerts("POST", "/api/alerts/evaluate", Map.of());
    }

    @Entonces("Alerts informa el código {string}")
    public void alertsCode(String code) {
        assertEquals(code, alertsReply.body().path("code").asText());
    }

    @Entonces("Alerts identifica los campos obligatorios:")
    public void alertsRequiredFields(DataTable table) {
        for (var row : table.asMaps(String.class, String.class)) {
            assertFalse(alertsReply.body().path("fields").path(row.get("campo")).asText().isBlank(), row.get("campo"));
        }
    }

    @Cuando("la regla térmica del punto cambia a {bigdecimal} a {bigdecimal} grados Celsius")
    public void changeRule(BigDecimal min, BigDecimal max) throws Exception {
        STACK.changeRule(point, min, max);
        expectedMin = min;
        expectedMax = max;
    }

    @Entonces("la alerta anterior conserva los límites de {bigdecimal} a {bigdecimal} grados Celsius")
    public void previousAlertLimits(BigDecimal min, BigDecimal max) throws Exception {
        assertNotNull(measurement);
        assertLimits(alertFor(measurement), min, max);
    }

    @Entonces("la nueva alerta usa los límites de {bigdecimal} a {bigdecimal} grados Celsius")
    public void newAlertLimits(BigDecimal min, BigDecimal max) throws Exception {
        assertNotNull(previousMeasurement);
        assertLimits(alertFor(measurement), min, max);
        assertFalse(alertFor(previousMeasurement).path("id").equals(alertFor(measurement).path("id")));
    }

    @Dado("Registry deja de estar disponible")
    public void stopRegistry() {
        STACK.stopRegistry();
    }

    @Cuando("Registry vuelve a estar disponible")
    public void recoverRegistry() throws Exception {
        STACK.startRegistry();
    }

    private JsonNode alertFor(JsonNode source) throws Exception {
        for (JsonNode alert : alerts()) {
            if (alert.path("measurementId").asText().equals(source.path("id").asText())) {
                return alert;
            }
        }
        throw new AssertionError("No existe alerta para la medición " + source.path("id").asText());
    }

    private static void assertLimits(JsonNode alert, BigDecimal min, BigDecimal max) {
        assertEquals(0, min.compareTo(alert.path("thresholdMin").decimalValue()), alert.toString());
        assertEquals(0, max.compareTo(alert.path("thresholdMax").decimalValue()), alert.toString());
    }

    private JsonNode alerts() throws Exception {
        var reply = STACK.alerts("GET", "/api/alerts?monitoringPointId=" + point, null);
        assertEquals(200, reply.status());
        assertTrue(reply.body().isArray());
        return reply.body();
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
