package pe.edu.upc.coldchain.telemetry.client;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pe.edu.upc.coldchain.telemetry.exception.MonitoringPointNotFoundException;
import pe.edu.upc.coldchain.telemetry.exception.RegistryUnavailableException;

@Component
public class RegistryClient {
    private final RestClient http;

    public RegistryClient(@Qualifier("registryRestClient") RestClient http) {
        this.http = http;
    }

    public void requirePoint(UUID id) {
        try {
            var response = http.get().uri("/api/v1/monitoring-points/{id}", id)
                    .retrieve().toEntity(PointReference.class);
            PointReference point = response.getBody();
            if (response.getStatusCode().value() != 200 || point == null || !id.equals(point.id())) {
                throw new RegistryUnavailableException();
            }
        } catch (HttpClientErrorException.NotFound exception) {
            throw new MonitoringPointNotFoundException();
        } catch (RestClientException exception) {
            throw new RegistryUnavailableException();
        }
    }

    public record PointReference(UUID id) { }
}
