package pe.edu.upc.coldchain;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upc.coldchain.model.MonitoringPoint;
import pe.edu.upc.coldchain.model.PointType;
import pe.edu.upc.coldchain.repository.PointRepository;
import pe.edu.upc.coldchain.security.AuthContext;
import pe.edu.upc.coldchain.security.IdentityClient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "coldchain.auth.enabled=true")
class PointAuthSecurityTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private PointRepository repository;

    @MockitoBean
    private IdentityClient identityClient;

    private UUID pointA;
    private UUID pointB;

    @BeforeEach
    void setup() {
        repository.deleteAll();
        pointA = repository.save(new MonitoringPoint("ALM-01", "Almacen 1", PointType.ALMACENAMIENTO)).getId();
        pointB = repository.save(new MonitoringPoint("ALM-02", "Almacen 2", PointType.ALMACENAMIENTO)).getId();
    }

    @Test
    void rejectsRequestsWithoutValidSession() throws Exception {
        mvc.perform(get("/api/v1/monitoring-points")).andExpect(status().isUnauthorized());

        when(identityClient.introspect("Bearer revoked-token"))
                .thenReturn(new IdentityClient.IntrospectionResult(401, null));
        mvc.perform(get("/api/v1/monitoring-points")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer revoked-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void enforcesPointPermissionsAndFiltersListForNonAdmin() throws Exception {
        AuthContext operator = new AuthContext(UUID.randomUUID(), "operador01", "OPERADOR", Set.of(pointA));
        when(identityClient.introspect("Bearer op-token"))
                .thenReturn(new IdentityClient.IntrospectionResult(200, operator));

        mvc.perform(get("/api/v1/monitoring-points/" + pointA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer op-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ALM-01"));

        mvc.perform(get("/api/v1/monitoring-points/" + pointB)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer op-token"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/monitoring-points")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer op-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("ALM-01"));
    }

    @Test
    void rejectsPointCreationForConductorRole() throws Exception {
        AuthContext driver = new AuthContext(UUID.randomUUID(), "conductor01", "CONDUCTOR", Set.of(pointA));
        when(identityClient.introspect("Bearer driver-token"))
                .thenReturn(new IdentityClient.IntrospectionResult(200, driver));

        mvc.perform(post("/api/v1/monitoring-points")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer driver-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ALM-03\",\"name\":\"Almacen 3\",\"type\":\"ALMACENAMIENTO\"}"))
                .andExpect(status().isForbidden());
    }
}
