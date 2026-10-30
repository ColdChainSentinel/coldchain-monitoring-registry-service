package pe.edu.upc.coldchain.telemetry;

import java.net.SocketTimeoutException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import pe.edu.upc.coldchain.telemetry.client.RegistryClient;
import pe.edu.upc.coldchain.telemetry.exception.MonitoringPointNotFoundException;
import pe.edu.upc.coldchain.telemetry.exception.RegistryUnavailableException;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RegistryClientTest {
    private final UUID point = UUID.randomUUID();
    private MockRestServiceServer server;
    private RegistryClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("http://registry");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RegistryClient(builder.build());
    }

    @Test
    void checksTheExistingRegistryRoute() {
        server.expect(requestTo("http://registry/api/v1/monitoring-points/" + point))
                .andRespond(withSuccess("{\"id\":\"" + point + "\",\"code\":\"CAM-01\"}", MediaType.APPLICATION_JSON));
        assertThatCode(() -> client.requirePoint(point)).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void mapsNotFound() {
        server.expect(requestTo("http://registry/api/v1/monitoring-points/" + point))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> client.requirePoint(point)).isInstanceOf(MonitoringPointNotFoundException.class);
    }

    @Test
    void mapsServerFailure() {
        server.expect(requestTo("http://registry/api/v1/monitoring-points/" + point))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThatThrownBy(() -> client.requirePoint(point)).isInstanceOf(RegistryUnavailableException.class);
    }

    @Test
    void rejectsUnexpectedSuccessfulStatus() {
        server.expect(requestTo("http://registry/api/v1/monitoring-points/" + point))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":\"" + point + "\"}"));
        assertThatThrownBy(() -> client.requirePoint(point)).isInstanceOf(RegistryUnavailableException.class);
    }

    @Test
    void mapsTransportTimeout() {
        server.expect(requestTo("http://registry/api/v1/monitoring-points/" + point))
                .andRespond(withException(new SocketTimeoutException("test")));
        assertThatThrownBy(() -> client.requirePoint(point)).isInstanceOf(RegistryUnavailableException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "{", "{\"id\":\"00000000-0000-0000-0000-000000000000\"}"})
    void rejectsInvalidSuccessfulResponse(String body) {
        server.expect(requestTo("http://registry/api/v1/monitoring-points/" + point))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.requirePoint(point)).isInstanceOf(RegistryUnavailableException.class);
    }
}
