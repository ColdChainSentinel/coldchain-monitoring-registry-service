package pe.edu.upc.coldchain.alerts.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(prefix = "coldchain.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IdentityClient {
    private final RestClient http;

    public IdentityClient(RestClient.Builder builder, AuthProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        this.http = builder.clone()
                .baseUrl(properties.identityBaseUrl().toString())
                .requestFactory(factory)
                .build();
    }

    public IntrospectionResult introspect(String authorizationHeader) {
        try {
            var response = http.get()
                    .uri("/api/v1/auth/introspect")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .toEntity(AuthContext.class);
            AuthContext context = response.getBody();
            if (response.getStatusCode().value() == 200 && context != null) {
                return new IntrospectionResult(200, context);
            }
            return new IntrospectionResult(503, null);
        } catch (HttpClientErrorException.Unauthorized ex) {
            return new IntrospectionResult(401, null);
        } catch (HttpClientErrorException.Forbidden ex) {
            return new IntrospectionResult(403, null);
        } catch (RestClientException ex) {
            return new IntrospectionResult(503, null);
        }
    }

    public record IntrospectionResult(int status, AuthContext context) {
    }
}
