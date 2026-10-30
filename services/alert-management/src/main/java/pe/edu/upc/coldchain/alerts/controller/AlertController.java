package pe.edu.upc.coldchain.alerts.controller;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.coldchain.alerts.dto.AlertResponse;
import pe.edu.upc.coldchain.alerts.dto.EvaluateMeasurementRequest;
import pe.edu.upc.coldchain.alerts.dto.EvaluationResponse;
import pe.edu.upc.coldchain.alerts.service.EvaluationService;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    private final EvaluationService service;

    public AlertController(EvaluationService service) {
        this.service = service;
    }

    @PostMapping("/evaluate")
    @Operation(operationId = "evaluateMeasurement", summary = "Evaluar medición de forma idempotente contra regla del punto")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Resultado original de evaluación, con o sin alerta"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(
                mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Punto inexistente", content = @Content(
                mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "409", description = "measurementId reutilizado con otros datos", content = @Content(
                mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "THERMAL_RULE_NOT_CONFIGURED", content = @Content(
                mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "503", description = "Registry o almacenamiento no disponibles", content = @Content(
                mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))})
    public EvaluationResponse evaluate(@Valid @RequestBody EvaluateMeasurementRequest request) {
        return service.evaluate(request);
    }

    @GetMapping
    @Operation(operationId = "listAlerts", summary = "Consultar alertas por punto, ordenadas por fecha y UUID")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Array, vacío si no hay alertas"),
        @ApiResponse(responseCode = "400", description = "UUID inválido o ausente", content = @Content(
                mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "503", description = "Almacenamiento no disponible", content = @Content(
                mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))})
    public List<AlertResponse> list(@RequestParam
            @Parameter(schema = @Schema(type = "string", format = "uuid"))
            @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
            String monitoringPointId) {
        return service.list(UUID.fromString(monitoringPointId));
    }
}
