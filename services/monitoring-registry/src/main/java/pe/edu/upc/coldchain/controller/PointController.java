package pe.edu.upc.coldchain.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import pe.edu.upc.coldchain.dto.CreatePointRequest;
import pe.edu.upc.coldchain.dto.PointResponse;
import pe.edu.upc.coldchain.service.PointService;

@RestController
@RequestMapping("/api/v1/monitoring-points")
public class PointController {
    private final PointService service;

    public PointController(PointService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Registrar un punto de monitoreo (HU-04)")
    public ResponseEntity<PointResponse> create(@Valid @RequestBody CreatePointRequest request) {
        PointResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/monitoring-points/" + response.id())).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar puntos ordenados por código")
    public List<PointResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un punto por UUID")
    public PointResponse find(@PathVariable UUID id) {
        return service.find(id);
    }
}
