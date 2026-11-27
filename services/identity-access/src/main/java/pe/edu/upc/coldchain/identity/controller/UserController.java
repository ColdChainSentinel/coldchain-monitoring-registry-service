package pe.edu.upc.coldchain.identity.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.coldchain.identity.dto.CreateUserRequest;
import pe.edu.upc.coldchain.identity.dto.UpdatePermissionsRequest;
import pe.edu.upc.coldchain.identity.dto.UpdateStatusRequest;
import pe.edu.upc.coldchain.identity.dto.UserResponse;
import pe.edu.upc.coldchain.identity.service.IdentityService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final IdentityService service;

    public UserController(IdentityService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Registrar una cuenta de usuario (HU-03)")
    public ResponseEntity<UserResponse> create(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody CreateUserRequest request) {
        service.requireAdmin(authorization);
        UserResponse created = service.createUser(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "Listar cuentas de usuario registradas")
    public List<UserResponse> list(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        service.requireAdmin(authorization);
        return service.listUsers();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar cuenta y permisos efectivos (HU-02)")
    public UserResponse find(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID id) {
        service.requireAdmin(authorization);
        return service.findUser(id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activar o desactivar una cuenta de usuario (HU-03)")
    public UserResponse updateStatus(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request) {
        UUID actingAdminId = service.requireAdmin(authorization).getId();
        return service.updateStatus(id, request.active(), actingAdminId);
    }

    @PutMapping("/{id}/permissions")
    @Operation(summary = "Asignar rol y puntos permitidos a una cuenta (HU-02)")
    public UserResponse updatePermissions(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePermissionsRequest request) {
        UUID actingAdminId = service.requireAdmin(authorization).getId();
        return service.updatePermissions(id, request, actingAdminId);
    }
}
