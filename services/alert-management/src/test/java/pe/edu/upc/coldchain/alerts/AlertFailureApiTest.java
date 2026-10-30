package pe.edu.upc.coldchain.alerts;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upc.coldchain.alerts.controller.AlertController;
import pe.edu.upc.coldchain.alerts.service.EvaluationService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertController.class)
class AlertFailureApiTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private EvaluationService service;

    @Test
    void readStorageFailureReturns503WithoutConnectionDetails() throws Exception {
        when(service.list(any())).thenThrow(new DataAccessResourceFailureException("secret connection details"));
        mvc.perform(get("/api/alerts").param("monitoringPointId", UUID.randomUUID().toString()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("El almacenamiento de evaluaciones y alertas no está disponible"));
    }

    @Test
    void writeStorageFailureReturns503() throws Exception {
        when(service.evaluate(any())).thenThrow(new DataAccessResourceFailureException("test"));
        mvc.perform(post("/api/alerts/evaluate").contentType(MediaType.APPLICATION_JSON).content("""
                {"measurementId":"0a41469b-9a1d-4245-a360-29986b37f44b",
                 "monitoringPointId":"b60d6bd0-e058-425d-9102-434b546e1284",
                 "temperature":9,"timestamp":"2026-10-29T10:00:00Z"}
                """))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.status").value(503));
    }
}
