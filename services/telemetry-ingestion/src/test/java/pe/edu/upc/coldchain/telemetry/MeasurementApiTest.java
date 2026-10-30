package pe.edu.upc.coldchain.telemetry;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
import pe.edu.upc.coldchain.telemetry.model.EvaluationStatus;
import pe.edu.upc.coldchain.telemetry.repository.MeasurementRepository;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("smoke")
class MeasurementApiTest {
    private static final String POINT = "b60d6bd0-e058-425d-9102-434b546e1284";
    private static final String API = "/api/measurements";
    private static final AtomicInteger REGISTRY_CALLS = new AtomicInteger();
    private static final AtomicInteger ALERTS_CALLS = new AtomicInteger();
    private static final AtomicReference<String> ALERTS_REQUEST = new AtomicReference<>();
    private static volatile int registryStatus;
    private static volatile int alertsStatus;
    private static volatile long registryDelay;
    private static volatile long alertsDelay;
    private static volatile Consumer<String> alertsObserver = body -> { };
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "telemetry-http-test");
        thread.setDaemon(true);
        return thread;
    });
    private static final HttpServer REGISTRY = startRegistry();
    private static final HttpServer ALERTS = startAlerts();

    @Autowired
    private TestRestTemplate http;
    @Autowired
    private MeasurementRepository repository;
    @Autowired
    private ObjectMapper mapper;

    @DynamicPropertySource
    static void clients(DynamicPropertyRegistry properties) {
        properties.add("coldchain.clients.registry-base-url", () -> "http://127.0.0.1:" + REGISTRY.getAddress().getPort());
        properties.add("coldchain.clients.alerts-base-url", () -> "http://127.0.0.1:" + ALERTS.getAddress().getPort());
        properties.add("coldchain.clients.connect-timeout", () -> "500ms");
        properties.add("coldchain.clients.read-timeout", () -> "500ms");
    }

    private static HttpServer newServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(EXECUTOR);
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static HttpServer startRegistry() {
        HttpServer server = newServer();
        server.createContext("/api/v1/monitoring-points/", exchange -> {
            REGISTRY_CALLS.incrementAndGet();
            pause(registryDelay);
            respond(exchange, registryStatus, "{\"id\":\"" + POINT + "\",\"code\":\"CAM-01\"}");
        });
        server.start();
        return server;
    }

    private static HttpServer startAlerts() {
        HttpServer server = newServer();
        server.createContext("/api/alerts/evaluate", exchange -> {
            ALERTS_CALLS.incrementAndGet();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            ALERTS_REQUEST.set(body);
            alertsObserver.accept(body);
            pause(alertsDelay);
            JsonNode measurement = new ObjectMapper().readTree(body);
            String result = alertsStatus == 422 ? "{\"code\":\"THERMAL_RULE_NOT_CONFIGURED\"}"
                    : "{\"measurementId\":\"" + measurement.get("measurementId").asText() + "\",\"outcome\":\"NO_ALERT\"}";
            respond(exchange, alertsStatus, result);
        });
        server.start();
        return server;
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
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

    @BeforeEach
    void reset() {
        repository.deleteAll();
        REGISTRY_CALLS.set(0);
        ALERTS_CALLS.set(0);
        ALERTS_REQUEST.set(null);
        registryStatus = 200;
        alertsStatus = 200;
        registryDelay = 0;
        alertsDelay = 0;
        alertsObserver = body -> { };
    }

    @AfterAll
    static void stopServers() {
        REGISTRY.stop(0);
        ALERTS.stop(0);
        EXECUTOR.shutdownNow();
    }

    private ResponseEntity<String> post(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return http.postForEntity(API, new HttpEntity<>(body, headers), String.class);
    }

    private String valid(String temperature, String timestamp) {
        return "{\"monitoringPointId\":\"" + POINT + "\",\"temperature\":" + temperature
                + ",\"timestamp\":\"" + timestamp + "\"}";
    }

    @Test
    void createsAndListsExactDecimalWithCanonicalUtcTimestamp() throws Exception {
        var response = post(valid("-12.123456789123456789", "2026-10-29T05:00:00.123456789-05:00"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var json = mapper.readTree(response.getBody());
        assertThat(UUID.fromString(json.get("id").asText())).isNotNull();
        assertThat(json.get("monitoringPointId").asText()).isEqualTo(POINT);
        assertThat(json.get("timestamp").asText()).isEqualTo("2026-10-29T10:00:00.123456Z");
        assertThat(json.get("evaluationStatus").asText()).isEqualTo("COMPLETED");
        assertThat(response.getBody()).contains("-12.123456789123456789");
        assertThat(repository.count()).isEqualTo(1);
        var list = http.getForEntity(API + "?monitoringPointId=" + POINT, String.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody()).contains("-12.123456789123456789");
        assertThat(mapper.readTree(list.getBody()).get(0).get("id")).isEqualTo(json.get("id"));
    }

    @Test
    void commitsPendingMeasurementBeforeSendingEvaluation() throws Exception {
        AtomicReference<EvaluationStatus> observed = new AtomicReference<>();
        alertsObserver = body -> {
            try {
                UUID id = UUID.fromString(mapper.readTree(body).get("measurementId").asText());
                observed.set(repository.findById(id).orElseThrow().getEvaluationStatus());
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        };
        var response = post(valid("0", "2026-10-29T10:00:00Z"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(observed.get()).isEqualTo(EvaluationStatus.PENDING);
        assertThat(mapper.readTree(ALERTS_REQUEST.get()).get("timestamp").asText()).isEqualTo("2026-10-29T10:00:00Z");
    }

    @Test
    void missingRequiredFieldsReturnProblemDetailsWithoutSideEffects() throws Exception {
        var response = post("{}");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        var problem = mapper.readTree(response.getBody());
        assertThat(problem.get("status").asInt()).isEqualTo(400);
        assertThat(problem.get("fields").has("monitoringPointId")).isTrue();
        assertThat(problem.get("fields").has("temperature")).isTrue();
        assertThat(problem.get("fields").has("timestamp")).isTrue();
        assertNoSideEffects();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "null", "[]", "{\"monitoringPointId\":null,\"temperature\":null,\"timestamp\":null}"})
    void rejectsMalformedOrNullBodies(String body) {
        assertThat(post(body).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertNoSideEffects();
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"3.5\"", "true", "{}", "NaN", "Infinity", "1e131072", "1e-16384", "100e2147483647"})
    void rejectsInvalidTemperatureWithoutPersisting(String temperature) {
        assertThat(post(valid(temperature, "2026-10-29T10:00:00Z")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertNoSideEffects();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-29T10:00:00", "2026-02-30T10:00:00Z", "2026-10-29", "invalid",
        "0000-01-01T00:00:00Z", "2026-10-29T10:00:00.1234567890Z", "2026-10-29T10:00:00+25:00"})
    void rejectsInvalidTimestamp(String timestamp) {
        assertThat(post(valid("3.5", timestamp)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertNoSideEffects();
    }

    @Test
    void rejectsEpochNumericTimestamp() {
        String body = "{\"monitoringPointId\":\"" + POINT + "\",\"temperature\":3.5,\"timestamp\":1793268000}";
        assertThat(post(body).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertNoSideEffects();
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "1-1-1-1-1", "sYgIAAAAQACAAAAAAAAAAA==", " "})
    void rejectsInvalidUuidInBodyAndQuery(String id) {
        assertThat(post(valid("3.5", "2026-10-29T10:00:00Z").replace(POINT, id)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(http.getForEntity(API + "?monitoringPointId={id}", String.class, id).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertNoSideEffects();
    }

    @Test
    void returnsEmptyArrayWithoutCallingRegistryOrAlerts() {
        registryStatus = 503;
        alertsStatus = 503;
        var response = http.getForEntity(API + "?monitoringPointId=" + UUID.randomUUID(), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("[]");
        assertNoSideEffects();
    }

    @Test
    void missingPointReturns404AndDoesNotStore() {
        registryStatus = 404;
        assertThat(post(valid("3.5", "2026-10-29T10:00:00Z")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(repository.count()).isZero();
        assertThat(ALERTS_CALLS).hasValue(0);
    }

    @Test
    void registryUnavailableReturns503AndDoesNotStore() {
        registryStatus = 503;
        assertThat(post(valid("3.5", "2026-10-29T10:00:00Z")).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(repository.count()).isZero();
        assertThat(ALERTS_CALLS).hasValue(0);
    }

    @Test
    void registryTimeoutReturns503AndDoesNotStore() {
        registryDelay = 1500;
        assertThat(post(valid("3.5", "2026-10-29T10:00:00Z")).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(repository.count()).isZero();
        assertThat(ALERTS_CALLS).hasValue(0);
    }

    @Test
    void alertsUnavailableReturns201AndStoresPending() {
        alertsStatus = 503;
        assertStoredEvaluation("PENDING");
    }

    @Test
    void alertsTimeoutReturns201AndStoresPending() {
        alertsDelay = 1500;
        assertStoredEvaluation("PENDING");
    }

    @Test
    void ruleMissingReturns201AndStoresRuleNotConfigured() {
        alertsStatus = 422;
        assertStoredEvaluation("RULE_NOT_CONFIGURED");
    }

    private void assertStoredEvaluation(String expectedStatus) {
        var response = post(valid("3.5", "2026-10-29T10:00:00Z"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("\"evaluationStatus\":\"" + expectedStatus + "\"");
        assertThat(repository.findAll()).singleElement()
                .satisfies(row -> assertThat(row.getEvaluationStatus().name()).isEqualTo(expectedStatus));
    }

    @Test
    void sortsFiltersAndAcceptsUppercaseUuid() throws Exception {
        post(valid("0", "2026-10-29T11:00:00Z"));
        post(valid("-10", "2026-10-29T09:00:00Z").replace(POINT, POINT.toUpperCase(java.util.Locale.ROOT)));
        var response = http.getForEntity(API + "?monitoringPointId=" + POINT.toUpperCase(java.util.Locale.ROOT), String.class);
        var rows = mapper.readTree(response.getBody());
        assertThat(rows.size()).isEqualTo(2);
        assertThat(rows.get(0).get("timestamp").asText()).isEqualTo("2026-10-29T09:00:00Z");
        assertThat(rows.get(1).get("timestamp").asText()).isEqualTo("2026-10-29T11:00:00Z");
        assertThat(http.getForEntity(API + "?monitoringPointId=" + UUID.randomUUID(), String.class).getBody())
                .isEqualTo("[]");
    }

    @Test
    void generatedOpenApiContainsTheImplementedPathsAndSchemas() throws Exception {
        var response = http.getForEntity("/v3/api-docs", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode docs = mapper.readTree(response.getBody());
        assertThat(docs.get("paths").get(API).has("post")).isTrue();
        assertThat(docs.get("paths").get(API).has("get")).isTrue();
        assertThat(docs.get("components").get("schemas").has("MeasurementResponse")).isTrue();
        assertThat(docs.get("info").get("version").asText()).isEqualTo("0.1.1");
        assertThat(docs.get("paths").get(API).get("post").get("responses").get("400")
                .get("content").has("application/problem+json")).isTrue();
        assertThat(docs.get("components").get("schemas").get("MeasurementResponse").get("required").toString())
                .contains("evaluationStatus", "monitoringPointId", "temperature", "timestamp", "id");
    }

    private void assertNoSideEffects() {
        assertThat(repository.count()).isZero();
        assertThat(REGISTRY_CALLS).hasValue(0);
        assertThat(ALERTS_CALLS).hasValue(0);
    }
}
