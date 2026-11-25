package pe.edu.upc.coldchain.identity.controller;

import java.util.UUID;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.coldchain.identity.dto.IntrospectResponse;
import pe.edu.upc.coldchain.identity.dto.LoginRequest;
import pe.edu.upc.coldchain.identity.dto.LoginResponse;
import pe.edu.upc.coldchain.identity.service.IdentityService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final IdentityService service;

    public AuthController(IdentityService service) {
        this.service = service;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión con credenciales (HU-01)")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión y revocar token activo (HU-01)")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        service.logout(authorization);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener perfil y permisos efectivos de la sesión actual")
    public IntrospectResponse me(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return service.introspect(authorization, null);
    }

    @GetMapping("/introspect")
    @Operation(summary = "Validar sesión y permiso opcional sobre un punto de monitoreo (HU-02)")
    public IntrospectResponse introspect(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(required = false) UUID monitoringPointId) {
        return service.introspect(authorization, monitoringPointId);
    }
}
