package pe.edu.upc.coldchain;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upc.coldchain.repository.PointRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PointApiTest {
    private static final String API = "/api/v1/monitoring-points";
    private static final String VALID = "{\"code\":\"CAM-01\",\"name\":\"Cámara 1\",\"type\":\"ALMACENAMIENTO\"}";
    @Autowired
    private MockMvc mvc;
    @Autowired
    private PointRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void registersListsAndRetrievesPoint() throws Exception {
        var response = mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.code").value("CAM-01")).andReturn();
        mvc.perform(get(response.getResponse().getHeader("Location")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Cámara 1"));
        mvc.perform(get(API)).andExpect(status().isOk()).andExpect(jsonPath("$[0].code").value("CAM-01"));
        assertEquals(1, repository.count());
    }

    @Test
    void duplicateCodeReturnsConflict() throws Exception {
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(VALID)).andExpect(status().isCreated());
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(VALID.replace("CAM-01", "cam-01")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Ya existe un punto con ese código"));
        assertEquals(1, repository.count());
    }

    @Test
    void requiredFieldsAreIdentifiedAndNotSaved() throws Exception {
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.code").exists())
                .andExpect(jsonPath("$.fields.name").exists()).andExpect(jsonPath("$.fields.type").exists());
        assertEquals(0, repository.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"code\": \" \", \"name\": \" \", \"type\": \"ALMACENAMIENTO\"}",
        "{\"code\":\"CAM 01\",\"name\":\"Cámara\",\"type\":\"ALMACENAMIENTO\"}",
        "{\"code\":\"CAM-01\",\"name\":\"Cámara\",\"type\":\"INVALIDO\"}",
        "{", "null"
    })
    void invalidBodiesAreRejected(String body) throws Exception {
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        assertEquals(0, repository.count());
    }

    @Test
    void missingPointReturnsNotFound() throws Exception {
        mvc.perform(get(API + "/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void invalidIdReturnsBadRequest() throws Exception {
        mvc.perform(get(API + "/invalid")).andExpect(status().isBadRequest());
    }

    @Test
    void emptyListReturnsAnArray() throws Exception {
        mvc.perform(get(API)).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void transportTypeIsAccepted() throws Exception {
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(VALID.replace("ALMACENAMIENTO", "TRANSPORTE")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.type").value("TRANSPORTE"));
    }

    @Test
    void longFieldsAreRejected() throws Exception {
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(VALID.replace("CAM-01", "A".repeat(41))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.code").exists());
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(VALID.replace("Cámara 1", "A".repeat(121))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.name").exists());
        assertEquals(0, repository.count());
    }

    @Test
    void openApiAndHealthAreAvailable() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.openapi").exists());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
}
