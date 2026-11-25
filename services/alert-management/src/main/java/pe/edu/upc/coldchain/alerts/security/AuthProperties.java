package pe.edu.upc.coldchain.alerts.security;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coldchain.auth")
public record AuthProperties(
        boolean enabled,
        URI identityBaseUrl,
        Duration connectTimeout,
        Duration readTimeout) {

    public AuthProperties {
        if (identityBaseUrl == null) {
            identityBaseUrl = URI.create("http://localhost:8083");
        }
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(2);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(5);
        }
    }
}
