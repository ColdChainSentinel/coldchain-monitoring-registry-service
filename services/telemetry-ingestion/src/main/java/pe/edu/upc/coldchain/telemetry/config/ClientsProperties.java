package pe.edu.upc.coldchain.telemetry.config;

import java.net.URI;
import java.time.Duration;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "coldchain.clients")
public record ClientsProperties(@NotNull URI registryBaseUrl, @NotNull URI alertsBaseUrl,
        @NotNull @DurationMin(millis = 1) Duration connectTimeout,
        @NotNull @DurationMin(millis = 1) Duration readTimeout) { }
