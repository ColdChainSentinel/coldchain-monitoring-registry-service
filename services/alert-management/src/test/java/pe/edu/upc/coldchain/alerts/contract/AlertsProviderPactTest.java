package pe.edu.upc.coldchain.alerts.contract;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import pe.edu.upc.coldchain.alerts.client.RegistryClient;
import pe.edu.upc.coldchain.alerts.model.ThermalRuleEntity;
import pe.edu.upc.coldchain.alerts.repository.AlertRepository;
import pe.edu.upc.coldchain.alerts.repository.EvaluationRepository;
import pe.edu.upc.coldchain.alerts.repository.ThermalRuleRepository;

/**
 * Verifica, contra la API real de Alert Management, los contratos de evaluación
 * que publica Telemetry Ingestion. Monitoring Registry se simula: su propio
 * contrato se verifica en RegistryProviderPactTest.
 */
@Provider("alert-management")
@PactFolder("../../contracts/pacts")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("smoke")
@TestPropertySource(properties = "coldchain.auth.enabled=false")
class AlertsProviderPactTest {
    private static final UUID POINT = UUID.fromString("7d0f2a4e-6a1b-4c8e-9f3d-2b5a8c1e4f60");

    @LocalServerPort
    int port;
    @MockitoBean
    RegistryClient registry;
    @Autowired
    ThermalRuleRepository rules;
    @Autowired
    AlertRepository alerts;
    @Autowired
    EvaluationRepository evaluations;

    @BeforeEach
    void target(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verify(PactVerificationContext context) {
        context.verifyInteraction();
    }

    private void reset() {
        evaluations.deleteAll();
        alerts.deleteAll();
        rules.deleteAll();
    }

    @State("el punto tiene un rango térmico de 2 a 8 °C")
    Map<String, Object> ruleConfigured() {
        reset();
        rules.save(new ThermalRuleEntity(POINT, new BigDecimal("2"), new BigDecimal("8"), "°C", Instant.now()));
        return Map.of("pointId", POINT.toString());
    }

    @State("el punto no tiene rango térmico")
    Map<String, Object> noRule() {
        reset();
        return Map.of("pointId", POINT.toString());
    }
}
