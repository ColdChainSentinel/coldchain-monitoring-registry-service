package pe.edu.upc.coldchain.telemetry.controller;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.coldchain.telemetry.dto.CreateMeasurementRequest;
import pe.edu.upc.coldchain.telemetry.dto.MeasurementResponse;
import pe.edu.upc.coldchain.telemetry.service.MeasurementService;

@RestController
@RequestMapping("/api/measurements")
public class MeasurementController {
    private final MeasurementService service;

    public MeasurementController(MeasurementService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "createMeasurement", summary = "Registrar una medición y solicitar evaluación HTTP")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Medición persistida; revisar evaluationStatus"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(
                mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Punto inexistente", content = @Content(
                mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "503", description = "Registry o almacenamiento no disponibles", content = @Content(
                mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))})
    public MeasurementResponse create(@Valid @RequestBody CreateMeasurementRequest request) {
        return service.create(request);
    }

    @GetMapping
    @Operation(operationId = "listMeasurements", summary = "Consultar mediciones por punto ordenadas por fecha y UUID")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Array, vacío si no hay datos"),
        @ApiResponse(responseCode = "400", description = "UUID inválido o ausente", content = @Content(
                mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "503", description = "Almacenamiento no disponible", content = @Content(
                mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))})
    public List<MeasurementResponse> list(@RequestParam
            @Parameter(schema = @Schema(type = "string", format = "uuid"))
            @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
            String monitoringPointId) {
        return service.list(UUID.fromString(monitoringPointId));
    }
}
