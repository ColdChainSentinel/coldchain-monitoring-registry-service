package pe.edu.upc.coldchain.alerts;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pe.edu.upc.coldchain.alerts.repository.AlertRepository;
import pe.edu.upc.coldchain.alerts.repository.EvaluationRepository;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("smoke")
class AlertApiTest {
    private static final String POINT = "b60d6bd0-e058-425d-9102-434b546e1284";
    private static final String NO_RULE_POINT = "a8362663-11ad-42fe-b97f-f98cadf361a9";
    private static final String EVALUATE = "/api/alerts/evaluate";
    private static final AtomicInteger REGISTRY_CALLS = new AtomicInteger();
    private static volatile int registryStatus = 200;
    private static volatile long registryDelay;
    private static volatile CountDownLatch registryGate;
    private static final ExecutorService SERVER_THREADS = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "alerts-registry-test");
        thread.setDaemon(true);
        return thread;
    });
    private static final HttpServer REGISTRY = startRegistry();
    @Autowired
    private TestRestTemplate http;
    @Autowired
    private AlertRepository alerts;
    @Autowired
    private EvaluationRepository evaluations;
    @Autowired
    private ObjectMapper mapper;

    private static HttpServer startRegistry() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(SERVER_THREADS);
            server.createContext("/api/v1/monitoring-points/", exchange -> {
                REGISTRY_CALLS.incrementAndGet();
                CountDownLatch gate = registryGate;
                try {
                    if (gate != null) {
                        gate.countDown();
                        gate.await(900, TimeUnit.MILLISECONDS);
                    }
                    Thread.sleep(registryDelay);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                String path = exchange.getRequestURI().getPath();
                String id = path.substring(path.lastIndexOf('/') + 1);
                respond(exchange, registryStatus, "{\"id\":\"" + id + "\",\"code\":\"CAM-01\"}");
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        try (exchange) {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        properties.add("coldchain.clients.registry-base-url", () -> "http://127.0.0.1:" + REGISTRY.getAddress().getPort());
        properties.add("coldchain.clients.connect-timeout", () -> "1s");
        properties.add("coldchain.clients.read-timeout", () -> "1s");
        properties.add("coldchain.alerts.rules." + POINT + ".threshold-min", () -> "2");
        properties.add("coldchain.alerts.rules." + POINT + ".threshold-max", () -> "8");
    }

    @BeforeEach
    void reset() {
        // This suite runs only against its isolated H2 or disposable TEST_DB_* database.
        evaluations.deleteAll();
        alerts.deleteAll();
        REGISTRY_CALLS.set(0);
        registryStatus = 200;
        registryDelay = 0;
        registryGate = null;
    }

    @AfterAll
    static void stopRegistry() {
        REGISTRY.stop(0);
        SERVER_THREADS.shutdownNow();
    }

    private ResponseEntity<String> post(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return http.postForEntity(EVALUATE, new HttpEntity<>(body, headers), String.class);
    }

    private String valid(UUID measurementId, String temperature) {
        return "{\"measurementId\":\"" + measurementId + "\",\"monitoringPointId\":\"" + POINT
                + "\",\"temperature\":" + temperature + ",\"timestamp\":\"2026-10-29T10:00:00.123456Z\"}";
    }

    @ParameterizedTest
    @CsvSource({"5,NO_ALERT,NONE", "2,NO_ALERT,NONE", "8.000,NO_ALERT,NONE",
        "1.999999999999999999,ALERT_CREATED,LOW_TEMPERATURE", "8.000000000000000001,ALERT_CREATED,HIGH_TEMPERATURE"})
    void evaluatesRangeAndPersistsOnlyActualAlerts(String temperature, String outcome, String type) throws Exception {
        UUID id = UUID.randomUUID();
        var response = post(valid(id, temperature));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode result = mapper.readTree(response.getBody());
        assertThat(result.get("measurementId").asText()).isEqualTo(id.toString());
        assertThat(result.get("outcome").asText()).isEqualTo(outcome);
        assertThat(evaluations.count()).isEqualTo(1);
        var list = http.getForEntity("/api/alerts?monitoringPointId=" + POINT, String.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        if ("NO_ALERT".equals(outcome)) {
            assertThat(result.has("alert")).isFalse();
            assertThat(alerts.count()).isZero();
            assertThat(list.getBody()).isEqualTo("[]");
        } else {
            assertThat(result.get("alert").get("type").asText()).isEqualTo(type);
            assertThat(result.get("alert").get("status").asText()).isEqualTo("OPEN");
            assertThat(result.get("alert").get("measurementId").asText()).isEqualTo(id.toString());
            assertThat(result.get("alert").get("monitoringPointId").asText()).isEqualTo(POINT);
            assertThat(result.get("alert").get("thresholdMin").decimalValue()).isEqualByComparingTo("2");
            assertThat(result.get("alert").get("thresholdMax").decimalValue()).isEqualByComparingTo("8");
            assertThat(response.getBody()).contains(temperature);
            assertThat(alerts.count()).isEqualTo(1);
            assertThat(mapper.readTree(list.getBody()).get(0)).isEqualTo(result.get("alert"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"5", "9"})
    void identicalReplayReturnsOriginalResultEvenWhenRegistryIsDown(String temperature) throws Exception {
        UUID id = UUID.randomUUID();
        String body = valid(id, temperature);
        var first = post(body);
        int calls = REGISTRY_CALLS.get();
        registryStatus = 503;
        var replay = post(body);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mapper.readTree(replay.getBody())).isEqualTo(mapper.readTree(first.getBody()));
        assertThat(evaluations.count()).isEqualTo(1);
        assertThat(alerts.count()).isEqualTo("9".equals(temperature) ? 1 : 0);
        assertThat(REGISTRY_CALLS).hasValue(calls);
    }

    @Test
    void equivalentDecimalScaleAndOffsetReplayMatchesOriginal() throws Exception {
        UUID id = UUID.randomUUID();
        var first = post(valid(id, "9"));
        String equivalent = valid(id, "9.0000").replace("2026-10-29T10:00:00.123456Z", "2026-10-29T05:00:00.123456789-05:00");
        var replay = post(equivalent);
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mapper.readTree(replay.getBody())).isEqualTo(mapper.readTree(first.getBody()));
        assertThat(alerts.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"temperature", "monitoringPointId", "timestamp"})
    void changedPayloadForExistingMeasurementConflicts(String field) {
        UUID id = UUID.randomUUID();
        String body = valid(id, "9");
        assertThat(post(body).getStatusCode()).isEqualTo(HttpStatus.OK);
        String other = switch (field) {
            case "temperature" -> valid(id, "1");
            case "monitoringPointId" -> body.replace(POINT, NO_RULE_POINT);
            default -> body.replace("10:00:00", "11:00:00");
        };
        var response = post(other);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(alerts.count()).isEqualTo(1);
        assertThat(evaluations.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"5", "9"})
    void concurrentIdenticalSubmissionsHaveOneEvaluationAndAtMostOneAlert(String temperature) throws Exception {
        int count = 6;
        UUID id = UUID.randomUUID();
        registryGate = new CountDownLatch(count);
        ExecutorService clients = Executors.newFixedThreadPool(count);
        try {
            List<Future<ResponseEntity<String>>> futures = clients.invokeAll(java.util.stream.IntStream.range(0, count)
                    .<java.util.concurrent.Callable<ResponseEntity<String>>>mapToObj(i -> () -> post(valid(id, temperature))).toList());
            JsonNode expected = null;
            for (var future : futures) {
                var response = future.get(15, TimeUnit.SECONDS);
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                JsonNode result = mapper.readTree(response.getBody());
                if (expected == null) {
                    expected = result;
                } else {
                    assertThat(result).isEqualTo(expected);
                }
            }
            assertThat(registryGate.getCount()).isZero();
            assertThat(evaluations.count()).isEqualTo(1);
            assertThat(alerts.count()).isEqualTo("9".equals(temperature) ? 1 : 0);
        } finally {
            clients.shutdownNow();
        }
    }

    @Test
    void concurrentConflictingPayloadsHaveOneWinnerAndNoOrphanAlert() throws Exception {
        UUID id = UUID.randomUUID();
        registryGate = new CountDownLatch(2);
        ExecutorService clients = Executors.newFixedThreadPool(2);
        try {
            var first = clients.submit(() -> post(valid(id, "1")));
            var second = clients.submit(() -> post(valid(id, "9")));
            assertThat(List.of(first.get(15, TimeUnit.SECONDS).getStatusCode(), second.get(15, TimeUnit.SECONDS).getStatusCode()))
                    .containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.CONFLICT);
            assertThat(registryGate.getCount()).isZero();
            assertThat(evaluations.count()).isEqualTo(1);
            assertThat(alerts.count()).isEqualTo(1);
        } finally {
            clients.shutdownNow();
        }
    }

    @Test
    void missingRuleReturns422WithStableCodeAndDoesNotPersist() throws Exception {
        var response = post(valid(UUID.randomUUID(), "9").replace(POINT, NO_RULE_POINT));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(mapper.readTree(response.getBody()).get("code").asText()).isEqualTo("THERMAL_RULE_NOT_CONFIGURED");
        assertThat(evaluations.count()).isZero();
        assertThat(alerts.count()).isZero();
    }

    @Test
    void nonexistentPointReturns404EvenIfThereIsNoRule() {
        registryStatus = 404;
        assertThat(post(valid(UUID.randomUUID(), "9").replace(POINT, NO_RULE_POINT)).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertNoRows();
    }

    @Test
    void registryFailureReturns503WithoutPersisting() {
        registryStatus = 503;
        assertThat(post(valid(UUID.randomUUID(), "9")).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertNoRows();
    }

    @Test
    void registryTimeoutReturns503WithoutPersisting() {
        registryDelay = 1500;
        assertThat(post(valid(UUID.randomUUID(), "9")).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertNoRows();
    }

    @Test
    void missingRequiredFieldsReturnProblemDetails() throws Exception {
        var response = post("{}");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        var fields = mapper.readTree(response.getBody()).get("fields");
        for (String name : List.of("measurementId", "monitoringPointId", "temperature", "timestamp")) {
            assertThat(fields.has(name)).isTrue();
        }
        assertNoRowsOrRegistryCalls();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "null", "[]", "{\"measurementId\":null,\"monitoringPointId\":null,\"temperature\":null,\"timestamp\":null}"})
    void rejectsMalformedOrNullBodies(String body) {
        assertThat(post(body).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertNoRowsOrRegistryCalls();
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"9\"", "true", "{}", "NaN", "Infinity", "1e131072", "1e-16384", "100e2147483647"})
    void rejectsInvalidTemperatureBeforeNetworkOrDbWrites(String value) {
        assertThat(post(valid(UUID.randomUUID(), value)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertNoRowsOrRegistryCalls();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-29T10:00:00", "2026-02-30T10:00:00Z", "invalid", "0000-01-01T00:00:00Z",
        "2026-10-29T10:00:00.1234567890Z"})
    void rejectsInvalidTimestamp(String value) {
        String body = valid(UUID.randomUUID(), "9").replace("2026-10-29T10:00:00.123456Z", value);
        assertThat(post(body).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertNoRowsOrRegistryCalls();
    }

    @Test
    void rejectsNumericEpochTimestamp() {
        String body = valid(UUID.randomUUID(), "9").replace("\"2026-10-29T10:00:00.123456Z\"", "1793268000");
        assertThat(post(body).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertNoRowsOrRegistryCalls();
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "1-1-1-1-1", "sYgIAAAAQACAAAAAAAAAAA==", " "})
    void rejectsNonCanonicalUuidInBodyAndQuery(String id) {
        UUID measurement = UUID.randomUUID();
        String body = valid(measurement, "9");
        assertThat(post(body.replace(POINT, id)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(post(body.replace(measurement.toString(), id)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(http.getForEntity("/api/alerts?monitoringPointId={id}", String.class, id).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertNoRowsOrRegistryCalls();
    }

    @Test
    void emptyQueryNeedsNoRegistry() {
        registryStatus = 503;
        var response = http.getForEntity("/api/alerts?monitoringPointId=" + UUID.randomUUID(), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("[]");
        assertNoRowsOrRegistryCalls();
    }

    @Test
    void filtersPointAndListsAlertsChronologically() throws Exception {
        post(valid(UUID.randomUUID(), "1"));
        post(valid(UUID.randomUUID(), "9"));
        var response = http.getForEntity("/api/alerts?monitoringPointId=" + POINT.toUpperCase(java.util.Locale.ROOT), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var rows = mapper.readTree(response.getBody());
        assertThat(rows.size()).isEqualTo(2);
        assertThat(rows.get(0).get("type").asText()).isEqualTo("LOW_TEMPERATURE");
        assertThat(rows.get(1).get("type").asText()).isEqualTo("HIGH_TEMPERATURE");
        assertThat(http.getForEntity("/api/alerts?monitoringPointId=" + NO_RULE_POINT, String.class).getBody()).isEqualTo("[]");
    }

    @Test
    void openApiExposesBothEndpointsAndTheOptionalAlertType() throws Exception {
        var response = http.getForEntity("/v3/api-docs", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode docs = mapper.readTree(response.getBody());
        assertThat(docs.get("info").get("version").asText()).isEqualTo("0.2.0");
        assertThat(docs.get("paths").get(EVALUATE).has("post")).isTrue();
        assertThat(docs.get("paths").get("/api/alerts").has("get")).isTrue();
        var schema = docs.get("components").get("schemas").get("AlertResponse");
        assertThat(schema.get("properties").has("type")).isTrue();
        assertThat(schema.get("required").toString()).doesNotContain("\"type\"");
    }

    private void assertNoRows() {
        assertThat(evaluations.count()).isZero();
        assertThat(alerts.count()).isZero();
    }

    private void assertNoRowsOrRegistryCalls() {
        assertNoRows();
        assertThat(REGISTRY_CALLS).hasValue(0);
    }
}
