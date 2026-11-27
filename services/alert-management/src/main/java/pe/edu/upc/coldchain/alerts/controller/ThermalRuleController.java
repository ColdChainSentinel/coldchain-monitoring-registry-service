package pe.edu.upc.coldchain.alerts.controller;

import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.coldchain.alerts.dto.ConfigureThermalRuleRequest;
import pe.edu.upc.coldchain.alerts.dto.ThermalRuleResponse;
import pe.edu.upc.coldchain.alerts.exception.ForbiddenAccessException;
import pe.edu.upc.coldchain.alerts.security.AuthContext;
import pe.edu.upc.coldchain.alerts.security.SessionAuthFilter;
import pe.edu.upc.coldchain.alerts.service.ThermalRuleService;

@RestController
@RequestMapping("/api/alerts/rules")
public class ThermalRuleController {
    private final ThermalRuleService service;

    public ThermalRuleController(ThermalRuleService service) {
        this.service = service;
    }

    @PutMapping("/{monitoringPointId}")
    @Operation(summary = "Configurar rango térmico mínimo y máximo en °C para un punto (HU-09)")
    public ThermalRuleResponse configure(@PathVariable UUID monitoringPointId,
            @Valid @RequestBody ConfigureThermalRuleRequest request,
            HttpServletRequest httpRequest) {
        AuthContext auth = authContext(httpRequest);
        if (auth != null && ("CONDUCTOR".equals(auth.role()) || !auth.canAccessPoint(monitoringPointId))) {
            throw new ForbiddenAccessException();
        }
        return service.configure(monitoringPointId, request);
    }

    @GetMapping("/{monitoringPointId}")
    @Operation(summary = "Consultar rango térmico vigente de un punto (HU-09)")
    public ResponseEntity<ThermalRuleResponse> find(@PathVariable UUID monitoringPointId,
            HttpServletRequest httpRequest) {
        AuthContext auth = authContext(httpRequest);
        if (auth != null && !auth.canAccessPoint(monitoringPointId)) {
            throw new ForbiddenAccessException();
        }
        return service.find(monitoringPointId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private AuthContext authContext(HttpServletRequest request) {
        Object attr = request.getAttribute(SessionAuthFilter.AUTH_CONTEXT_ATTR);
        return attr instanceof AuthContext context ? context : null;
    }
}
