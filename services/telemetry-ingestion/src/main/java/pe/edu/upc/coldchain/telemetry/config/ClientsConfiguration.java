package pe.edu.upc.coldchain.telemetry.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ClientsProperties.class)
public class ClientsConfiguration {
    @Bean
    RestClient registryRestClient(RestClient.Builder builder, ClientsProperties properties) {
        return createClient(builder, properties.registryBaseUrl().toString(), properties);
    }

    @Bean
    RestClient alertsRestClient(RestClient.Builder builder, ClientsProperties properties) {
        return createClient(builder, properties.alertsBaseUrl().toString(), properties);
    }

    private RestClient createClient(RestClient.Builder builder, String url, ClientsProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        return builder.clone().baseUrl(url).requestFactory(factory).build();
    }
}
