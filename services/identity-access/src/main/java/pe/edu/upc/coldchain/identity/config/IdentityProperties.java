package pe.edu.upc.coldchain.identity.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coldchain.identity")
public record IdentityProperties(
        Duration sessionTtl,
        BootstrapAdmin bootstrapAdmin) {

    public IdentityProperties {
        if (sessionTtl == null) {
            sessionTtl = Duration.ofHours(12);
        }
        if (bootstrapAdmin == null) {
            bootstrapAdmin = new BootstrapAdmin("", "", "Administrador ColdChain");
        }
    }

    public record BootstrapAdmin(String username, String password, String fullName) {
    }
}
