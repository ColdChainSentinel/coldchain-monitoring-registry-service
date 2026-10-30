package pe.edu.upc.coldchain.alerts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "ColdChain Sentinel - Alert Management", version = "0.2.0"))
public class AlertsApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlertsApplication.class, args);
    }
}
