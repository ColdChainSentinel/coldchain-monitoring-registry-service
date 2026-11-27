package pe.edu.upc.coldchain.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
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
import pe.edu.upc.coldchain.exception.ForbiddenAccessException;
import pe.edu.upc.coldchain.security.AuthContext;
import pe.edu.upc.coldchain.security.IdentityClient;
import pe.edu.upc.coldchain.security.SessionAuthFilter;
import pe.edu.upc.coldchain.service.PointService;

@RestController
@RequestMapping("/api/v1/monitoring-points")
public class PointController {
    private final PointService service;
    private final ObjectProvider<IdentityClient> identityClientProvider;

    public PointController(PointService service, ObjectProvider<IdentityClient> identityClientProvider) {
        this.service = service;
        this.identityClientProvider = identityClientProvider;
    }

    @PostMapping
    @Operation(summary = "Registrar un punto de monitoreo (HU-04)")
    public ResponseEntity<PointResponse> create(@Valid @RequestBody CreatePointRequest request,
            HttpServletRequest httpRequest) {
        AuthContext auth = authContext(httpRequest);
        if (auth != null && "CONDUCTOR".equals(auth.role())) {
            throw new ForbiddenAccessException();
        }
        PointResponse response = service.create(request);
        if (auth != null && !auth.isAdmin()) {
            IdentityClient client = identityClientProvider.getIfAvailable();
            if (client != null) {
                client.grantPointAccess(httpRequest.getHeader(HttpHeaders.AUTHORIZATION), response.id());
            }
        }
        return ResponseEntity.created(URI.create("/api/v1/monitoring-points/" + response.id())).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar puntos ordenados por código")
    public List<PointResponse> list(HttpServletRequest httpRequest) {
        AuthContext auth = authContext(httpRequest);
        List<PointResponse> all = service.list();
        if (auth == null || auth.isAdmin()) {
            return all;
        }
        return all.stream().filter(point -> auth.canAccessPoint(point.id())).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un punto por UUID")
    public PointResponse find(@PathVariable UUID id, HttpServletRequest httpRequest) {
        AuthContext auth = authContext(httpRequest);
        if (auth != null && !auth.canAccessPoint(id)) {
            throw new ForbiddenAccessException();
        }
        return service.find(id);
    }

    private AuthContext authContext(HttpServletRequest request) {
        Object attr = request.getAttribute(SessionAuthFilter.AUTH_CONTEXT_ATTR);
        return attr instanceof AuthContext context ? context : null;
    }
}
