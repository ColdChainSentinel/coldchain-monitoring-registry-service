package pe.edu.upc.coldchain.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.coldchain.dto.CreateDeviceRequest;
import pe.edu.upc.coldchain.dto.DeviceResponse;
import pe.edu.upc.coldchain.exception.ForbiddenAccessException;
import pe.edu.upc.coldchain.security.AuthContext;
import pe.edu.upc.coldchain.security.SessionAuthFilter;
import pe.edu.upc.coldchain.service.DeviceService;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {
    private final DeviceService service;

    public DeviceController(DeviceService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Registrar un dispositivo de temperatura (HU-06)")
    public ResponseEntity<DeviceResponse> create(@Valid @RequestBody CreateDeviceRequest request,
            HttpServletRequest httpRequest) {
        AuthContext auth = authContext(httpRequest);
        if (auth != null && ("CONDUCTOR".equals(auth.role()) || !auth.canAccessPoint(request.monitoringPointId()))) {
            throw new ForbiddenAccessException();
        }
        DeviceResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/devices/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "Listar dispositivos registrados")
    public List<DeviceResponse> list(@RequestParam(required = false) UUID monitoringPointId,
            HttpServletRequest httpRequest) {
        AuthContext auth = authContext(httpRequest);
        if (monitoringPointId != null && auth != null && !auth.canAccessPoint(monitoringPointId)) {
            throw new ForbiddenAccessException();
        }
        List<DeviceResponse> devices = service.list(monitoringPointId);
        if (auth == null || auth.isAdmin()) {
            return devices;
        }
        return devices.stream().filter(device -> auth.canAccessPoint(device.monitoringPointId())).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar dispositivo por UUID (HU-06)")
    public DeviceResponse find(@PathVariable UUID id, HttpServletRequest httpRequest) {
        DeviceResponse device = service.find(id);
        AuthContext auth = authContext(httpRequest);
        if (auth != null && !auth.canAccessPoint(device.monitoringPointId())) {
            throw new ForbiddenAccessException();
        }
        return device;
    }

    private AuthContext authContext(HttpServletRequest request) {
        Object attr = request.getAttribute(SessionAuthFilter.AUTH_CONTEXT_ATTR);
        return attr instanceof AuthContext context ? context : null;
    }
}
