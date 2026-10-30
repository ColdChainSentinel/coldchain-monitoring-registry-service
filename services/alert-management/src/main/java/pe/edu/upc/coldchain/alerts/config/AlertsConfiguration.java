package pe.edu.upc.coldchain.alerts.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({ThermalRulesProperties.class, RegistryProperties.class})
public class AlertsConfiguration {
    @Bean
    RestClient registryRestClient(RestClient.Builder builder, RegistryProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        return builder.baseUrl(properties.registryBaseUrl().toString()).requestFactory(factory).build();
    }

    @Bean
    Clock alertsClock() {
        return Clock.systemUTC();
    }
}
