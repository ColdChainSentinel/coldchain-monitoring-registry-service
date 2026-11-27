package pe.edu.upc.coldchain.identity;

import java.util.Set;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upc.coldchain.identity.repository.UserAccountRepository;
import pe.edu.upc.coldchain.identity.repository.UserSessionRepository;
import pe.edu.upc.coldchain.identity.service.IdentityService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdentityApiTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserSessionRepository sessionRepository;

    @Autowired
    private UserAccountRepository userRepository;

    @Autowired
    private IdentityService identityService;

    @BeforeEach
    void resetDatabase() {
        sessionRepository.deleteAll();
        userRepository.deleteAll();
        identityService.ensureBootstrapUser("admin", "AdminTestPassword123", "Administrador Test");
    }

    @Test
    void loginLogoutAndGenericInvalidErrorWorkAsExpected() throws Exception {
        String adminToken = loginAs("admin", "AdminTestPassword123");

        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        String wrongPasswordDetail = failedLoginDetail("admin", "WrongPass999");
        String missingUserDetail = failedLoginDetail("nobody99", "WrongPass999");
        assertEquals(wrongPasswordDetail, missingUserDetail);

        mvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userLifecycleDeactivationRevokesSessionsAndRejectsLoginWithSameError() throws Exception {
        String adminToken = loginAs("admin", "AdminTestPassword123");
        String createBody = """
                {
                  "username": "conductor01",
                  "password": "DriverPassword123",
                  "fullName": "Carlos Conductor",
                  "role": "CONDUCTOR",
                  "allowedPointIds": []
                }
                """;
        String createdJson = mvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("conductor01"))
                .andExpect(jsonPath("$.role").value("CONDUCTOR"))
                .andReturn().getResponse().getContentAsString();
        String userId = JSON.readTree(createdJson).get("id").asText();

        mvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isConflict());

        String driverToken = loginAs("conductor01", "DriverPassword123");

        mvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + driverToken))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/v1/users/" + userId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + driverToken))
                .andExpect(status().isUnauthorized());

        String inactiveDetail = failedLoginDetail("conductor01", "DriverPassword123");
        String wrongPassDetail = failedLoginDetail("admin", "OtherPass123");
        assertEquals(wrongPassDetail, inactiveDetail);
    }

    @Test
    void pointPermissionsAreEnforcedAndUpdatedImmediately() throws Exception {
        String adminToken = loginAs("admin", "AdminTestPassword123");
        UUID alm01 = UUID.randomUUID();
        UUID alm02 = UUID.randomUUID();

        String createBody = """
                {
                  "username": "operador01",
                  "password": "OperatorPass123",
                  "fullName": "Ana Operadora",
                  "role": "OPERADOR",
                  "allowedPointIds": ["%s"]
                }
                """.formatted(alm01);

        String createdJson = mvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String operatorId = JSON.readTree(createdJson).get("id").asText();

        String operatorToken = loginAs("operador01", "OperatorPass123");

        mvc.perform(get("/api/v1/auth/introspect")
                        .param("monitoringPointId", alm01.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/auth/introspect")
                        .param("monitoringPointId", alm02.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/v1/users/" + operatorId + "/permissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"OPERADOR\",\"allowedPointIds\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedPointIds").isEmpty());

        mvc.perform(get("/api/v1/auth/introspect")
                        .param("monitoringPointId", alm01.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotDeactivateOrDemoteOwnAccount() throws Exception {
        String adminToken = loginAs("admin", "AdminTestPassword123");
        String meJson = mvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String adminId = JSON.readTree(meJson).get("userId").asText();

        mvc.perform(patch("/api/v1/users/" + adminId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/v1/users/" + adminId + "/permissions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"OPERADOR\",\"allowedPointIds\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void supervisorCanGrantOwnAccessToCreatedPointWhileConductorIsRejected() throws Exception {
        String adminToken = loginAs("admin", "AdminTestPassword123");
        UUID newPoint = UUID.randomUUID();

        mvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"sup01\",\"password\":\"SuperPass123\",\"fullName\":\"Sup Uno\",\"role\":\"SUPERVISOR\",\"allowedPointIds\":[]}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"cond01\",\"password\":\"DriverPass123\",\"fullName\":\"Cond Uno\",\"role\":\"CONDUCTOR\",\"allowedPointIds\":[]}"))
                .andExpect(status().isCreated());

        String supToken = loginAs("sup01", "SuperPass123");
        String condToken = loginAs("cond01", "DriverPass123");

        mvc.perform(post("/api/v1/auth/me/points/" + newPoint)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + condToken))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/auth/me/points/" + newPoint)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + supToken))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/auth/introspect")
                        .param("monitoringPointId", newPoint.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + supToken))
                .andExpect(status().isOk());
    }

    @Test
    void healthAndOpenApiArePublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.openapi").exists());
    }

    private String loginAs(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        String json = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Inicio de sesión exitoso"))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = JSON.readTree(json);
        return node.get("token").asText();
    }

    private String failedLoginDetail(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        String json = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        return JSON.readTree(json).get("detail").asText();
    }
}
