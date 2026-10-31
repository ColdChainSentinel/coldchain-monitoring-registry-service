package pe.edu.upc.coldchain.acceptance;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pe.edu.upc.coldchain.MonitoringApplication;

@CucumberContextConfiguration
@SpringBootTest(classes = MonitoringApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("acceptance")
public class AcceptanceSpringConfiguration { }
