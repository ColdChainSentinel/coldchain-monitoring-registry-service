package pe.edu.upc.coldchain.telemetry;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upc.coldchain.telemetry.controller.MeasurementController;
import pe.edu.upc.coldchain.telemetry.service.MeasurementService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeasurementController.class)
class MeasurementFailureApiTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private MeasurementService service;

    @Test
    void storageReadFailureReturns503WithoutInternalDetails() throws Exception {
        when(service.list(any())).thenThrow(new DataAccessResourceFailureException("secret connection details"));
        mvc.perform(get("/api/measurements").param("monitoringPointId", UUID.randomUUID().toString()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("El almacenamiento de mediciones no está disponible"));
    }

    @Test
    void storageCreateFailureReturns503() throws Exception {
        when(service.create(any())).thenThrow(new DataAccessResourceFailureException("test"));
        mvc.perform(post("/api/measurements").contentType(MediaType.APPLICATION_JSON).content("""
                {"monitoringPointId":"b60d6bd0-e058-425d-9102-434b546e1284",
                 "temperature":3.5,"timestamp":"2026-10-29T10:00:00Z"}
                """))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.status").value(503));
    }
}
