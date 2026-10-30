package pe.edu.upc.coldchain.telemetry;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import pe.edu.upc.coldchain.telemetry.client.AlertsClient;
import pe.edu.upc.coldchain.telemetry.dto.MeasurementResponse;
import pe.edu.upc.coldchain.telemetry.model.EvaluationStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AlertsClientTest {
    private final MeasurementResponse measurement = new MeasurementResponse(UUID.randomUUID(), UUID.randomUUID(),
            new BigDecimal("3.125"), Instant.parse("2026-10-29T10:00:00Z"), EvaluationStatus.PENDING);
    private MockRestServiceServer server;
    private AlertsClient client;

    @BeforeEach
    void setUp() {
        var mapper = JsonMapper.builder().addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
        var builder = RestClient.builder().baseUrl("http://alerts")
                .messageConverters(converters -> {
                    converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                    converters.add(new MappingJackson2HttpMessageConverter(mapper));
                });
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AlertsClient(builder.build());
    }

    @Test
    void sendsContractPayloadAndAcceptsNoAlert() {
        server.expect(requestTo("http://alerts/api/alerts/evaluate")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.measurementId").value(measurement.id().toString()))
                .andExpect(jsonPath("$.monitoringPointId").value(measurement.monitoringPointId().toString()))
                .andExpect(jsonPath("$.temperature").value(3.125))
                .andExpect(jsonPath("$.timestamp").value("2026-10-29T10:00:00Z"))
                .andExpect(jsonPath("$.thresholdMin").doesNotExist())
                .andRespond(withSuccess("{\"measurementId\":\"" + measurement.id()
                        + "\",\"outcome\":\"NO_ALERT\"}", MediaType.APPLICATION_JSON));
        assertThat(client.evaluate(measurement)).isEqualTo(EvaluationStatus.COMPLETED);
        server.verify();
    }

    @Test
    void acceptsAlertCreatedWithMatchingRequiredData() {
        String body = """
                {"measurementId":"%s","outcome":"ALERT_CREATED","alert":{
                 "id":"%s","monitoringPointId":"%s","measurementId":"%s","temperature":3.125,
                 "thresholdMin":4,"thresholdMax":8,"status":"OPEN","createdAt":"2026-10-29T11:00:00Z"}}
                """.formatted(measurement.id(), UUID.randomUUID(), measurement.monitoringPointId(), measurement.id());
        server.expect(requestTo("http://alerts/api/alerts/evaluate"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertThat(client.evaluate(measurement)).isEqualTo(EvaluationStatus.COMPLETED);
    }

    @Test
    void distinguishesMissingRule() {
        server.expect(requestTo("http://alerts/api/alerts/evaluate"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("{\"code\":\"THERMAL_RULE_NOT_CONFIGURED\"}"));
        assertThat(client.evaluate(measurement)).isEqualTo(EvaluationStatus.RULE_NOT_CONFIGURED);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 409, 422, 500, 503})
    void keepsPendingOnUnexpectedHttpFailure(int status) {
        server.expect(requestTo("http://alerts/api/alerts/evaluate"))
                .andRespond(withStatus(HttpStatus.valueOf(status)).contentType(MediaType.APPLICATION_JSON).body("{}"));
        assertThat(client.evaluate(measurement)).isEqualTo(EvaluationStatus.PENDING);
    }

    @Test
    void keepsPendingOnTimeout() {
        server.expect(requestTo("http://alerts/api/alerts/evaluate"))
                .andRespond(withException(new SocketTimeoutException("test")));
        assertThat(client.evaluate(measurement)).isEqualTo(EvaluationStatus.PENDING);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{", "{}", "{\"outcome\":\"NO_ALERT\"}",
        "{\"measurementId\":\"00000000-0000-0000-0000-000000000000\",\"outcome\":\"NO_ALERT\"}"})
    void keepsPendingOnInvalidSuccessBody(String body) {
        server.expect(requestTo("http://alerts/api/alerts/evaluate"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertThat(client.evaluate(measurement)).isEqualTo(EvaluationStatus.PENDING);
    }
}
