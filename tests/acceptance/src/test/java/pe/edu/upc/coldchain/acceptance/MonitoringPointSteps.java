package pe.edu.upc.coldchain.acceptance;

import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import pe.edu.upc.coldchain.repository.PointRepository;
import static org.assertj.core.api.Assertions.assertThat;

public class MonitoringPointSteps {
    private static final String API = "/api/v1/monitoring-points";
    @Autowired
    private TestRestTemplate http;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private PointRepository repository;
    private ResponseEntity<String> response;
    private JsonNode submitted;
    private JsonNode original;

    @Before
    public void resetScenario() {
        repository.deleteAll();
        response = null;
        submitted = null;
        original = null;
    }

    @After
    public void cleanScenario() {
        repository.deleteAll();
    }

    @Dado("que el registro de puntos está vacío")
    public void emptyRegistry() throws Exception {
        assertThat(list()).isEmpty();
    }

    @Dado("que existe el siguiente punto:")
    public void existingPoint(DataTable table) throws Exception {
        registerPoint(table);
        responseStatus(201);
        original = body(response);
    }

    @Cuando("registro el siguiente punto:")
    public void registerPoint(DataTable table) {
        var rows = table.asMaps(String.class, String.class);
        assertThat(rows).hasSize(1);
        Map<String, String> point = rows.get(0);
        assertThat(point).containsOnlyKeys("code", "name", "type");
        submitted = mapper.valueToTree(point);
        post(point);
    }

    @Cuando("envío un registro sin datos")
    public void registerWithoutData() {
        post(Map.of());
    }

    @Entonces("la API responde con estado {int}")
    public void responseStatus(int status) {
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().value()).isEqualTo(status);
    }

    @Entonces("el punto creado se puede consultar y aparece en el listado con sus datos")
    public void createdPointIsQueryable() throws Exception {
        JsonNode created = body(response);
        String id = created.path("id").asText();
        assertThat(UUID.fromString(id).toString()).isEqualTo(id);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        String location = response.getHeaders().getLocation().toString();
        assertThat(location).isEqualTo(API + "/" + id);
        assertPointData(created, submitted);
        var query = http.getForEntity(location, String.class);
        assertThat(query.getStatusCode().value()).isEqualTo(200);
        assertThat(body(query)).isEqualTo(created);
        JsonNode points = list();
        assertThat(points.size()).isEqualTo(1);
        assertThat(points.get(0)).isEqualTo(created);
    }

    @Entonces("existen {int} puntos registrados")
    public void pointCount(int expected) throws Exception {
        assertThat(list().size()).isEqualTo(expected);
    }

    @Entonces("se informa {string}")
    public void errorDetail(String expected) throws Exception {
        assertThat(body(response).path("detail").asText()).isEqualTo(expected);
    }

    @Entonces("el listado conserva únicamente el punto original")
    public void originalPointIsUnchanged() throws Exception {
        assertThat(original).isNotNull();
        JsonNode points = list();
        assertThat(points.size()).isEqualTo(1);
        assertThat(points.get(0)).isEqualTo(original);
    }

    @Entonces("se identifican los siguientes campos obligatorios:")
    public void requiredFields(DataTable table) throws Exception {
        JsonNode fields = body(response).path("fields");
        assertThat(fields.isObject()).isTrue();
        for (var row : table.asMaps(String.class, String.class)) {
            assertThat(fields.path(row.get("campo")).asText()).isNotBlank();
        }
    }

    private void post(Map<String, String> point) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        response = http.postForEntity(API, new HttpEntity<>(point, headers), String.class);
    }

    private JsonNode list() throws Exception {
        var query = http.getForEntity(API, String.class);
        assertThat(query.getStatusCode().value()).isEqualTo(200);
        JsonNode points = body(query);
        assertThat(points.isArray()).isTrue();
        return points;
    }

    private JsonNode body(ResponseEntity<String> result) throws Exception {
        assertThat(result.getBody()).isNotBlank();
        return mapper.readTree(result.getBody());
    }

    private void assertPointData(JsonNode actual, JsonNode expected) {
        for (String field : new String[]{"code", "name", "type"}) {
            assertThat(actual.path(field)).as(field).isEqualTo(expected.path(field));
        }
    }
}
