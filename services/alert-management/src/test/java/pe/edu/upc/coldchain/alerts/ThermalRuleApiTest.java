package pe.edu.upc.coldchain.alerts;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upc.coldchain.alerts.client.RegistryClient;
import pe.edu.upc.coldchain.alerts.repository.AlertRepository;
import pe.edu.upc.coldchain.alerts.repository.EvaluationRepository;
import pe.edu.upc.coldchain.alerts.repository.ThermalRuleRepository;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("smoke")
@TestPropertySource(properties = "coldchain.auth.enabled=false")
class ThermalRuleApiTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ThermalRuleRepository ruleRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private AlertRepository alertRepository;

    @MockitoBean
    private RegistryClient registryClient;

    private UUID pointId;

    @BeforeEach
    void setup() {
        evaluationRepository.deleteAll();
        alertRepository.deleteAll();
        ruleRepository.deleteAll();
        pointId = UUID.randomUUID();
        doNothing().when(registryClient).requirePoint(pointId);
    }

    @Test
    void configuresValidRuleRejectsInvalidOrderAndEvaluatesInclusiveLimits() throws Exception {
        mvc.perform(put("/api/alerts/rules/" + pointId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"thresholdMin\":2,\"thresholdMax\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thresholdMin").value(2))
                .andExpect(jsonPath("$.thresholdMax").value(8))
                .andExpect(jsonPath("$.unit").value("°C"));

        mvc.perform(get("/api/alerts/rules/" + pointId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thresholdMin").value(2))
                .andExpect(jsonPath("$.thresholdMax").value(8))
                .andExpect(jsonPath("$.unit").value("°C"));

        mvc.perform(put("/api/alerts/rules/" + pointId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"thresholdMin\":8,\"thresholdMax\":2}"))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/alerts/rules/" + pointId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"thresholdMin\":5,\"thresholdMax\":5}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/alerts/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "measurementId": "%s",
                                  "monitoringPointId": "%s",
                                  "temperature": 2,
                                  "timestamp": "2026-11-24T21:00:00Z"
                                }
                                """.formatted(UUID.randomUUID(), pointId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("NO_ALERT"));

        mvc.perform(post("/api/alerts/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "measurementId": "%s",
                                  "monitoringPointId": "%s",
                                  "temperature": 8,
                                  "timestamp": "2026-11-24T21:01:00Z"
                                }
                                """.formatted(UUID.randomUUID(), pointId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("NO_ALERT"));
    }
}
