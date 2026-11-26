package pe.edu.upc.coldchain.contract;

import java.util.Map;
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
import pe.edu.upc.coldchain.model.MonitoringPoint;
import pe.edu.upc.coldchain.model.PointType;
import pe.edu.upc.coldchain.repository.DeviceRepository;
import pe.edu.upc.coldchain.repository.PointRepository;

/**
 * Verifica, contra la API real de Monitoring Registry, los contratos que publica
 * Telemetry Ingestion en contracts/pacts.
 */
@Provider("monitoring-registry")
@PactFolder("../../contracts/pacts")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RegistryProviderPactTest {
    @LocalServerPort
    int port;
    @Autowired
    PointRepository points;
    @Autowired
    DeviceRepository devices;

    @BeforeEach
    void target(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verify(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("existe un punto de monitoreo")
    Map<String, Object> existingPoint() {
        devices.deleteAll();
        points.deleteAll();
        var point = points.save(new MonitoringPoint("PACT-01", "Cámara de contrato", PointType.ALMACENAMIENTO));
        return Map.of("pointId", point.getId().toString());
    }

    @State("no existe el punto de monitoreo")
    void missingPoint() {
        devices.deleteAll();
        points.deleteAll();
    }
}
