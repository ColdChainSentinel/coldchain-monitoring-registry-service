package pe.edu.upc.coldchain;

import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upc.coldchain.model.MonitoringPoint;
import pe.edu.upc.coldchain.model.PointType;
import pe.edu.upc.coldchain.repository.DeviceRepository;
import pe.edu.upc.coldchain.repository.PointRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeviceApiTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private PointRepository pointRepository;

    private UUID activePointId;
    private UUID inactivePointId;

    @BeforeEach
    void setup() {
        deviceRepository.deleteAll();
        pointRepository.deleteAll();
        activePointId = pointRepository.save(new MonitoringPoint("ALM-01", "Almacen 1", PointType.ALMACENAMIENTO))
                .getId();
        MonitoringPoint inactive = new MonitoringPoint("ALM-09", "Almacen Inactivo", PointType.ALMACENAMIENTO);
        inactive.setActive(false);
        inactivePointId = pointRepository.save(inactive).getId();
    }

    @Test
    void registersQueriesAndRejectsDuplicateOrInactivePoint() throws Exception {
        String createJson = """
                {
                  "code": "SEN-01",
                  "monitoringPointId": "%s"
                }
                """.formatted(activePointId);

        String responseJson = mvc.perform(post("/api/v1/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SEN-01"))
                .andExpect(jsonPath("$.status").value("Activo"))
                .andExpect(jsonPath("$.monitoringPointCode").value("ALM-01"))
                .andReturn().getResponse().getContentAsString();

        String deviceId = JSON.readTree(responseJson).get("id").asText();

        mvc.perform(get("/api/v1/devices/" + deviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SEN-01"))
                .andExpect(jsonPath("$.status").value("Activo"))
                .andExpect(jsonPath("$.monitoringPointId").value(activePointId.toString()));

        mvc.perform(post("/api/v1/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isConflict());
        assertEquals(1, deviceRepository.count());

        String inactiveBody = """
                {
                  "code": "SEN-02",
                  "monitoringPointId": "%s"
                }
                """.formatted(inactivePointId);

        mvc.perform(post("/api/v1/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inactiveBody))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("El punto de monitoreo no está activo"));
        assertEquals(1, deviceRepository.count());
    }
}
