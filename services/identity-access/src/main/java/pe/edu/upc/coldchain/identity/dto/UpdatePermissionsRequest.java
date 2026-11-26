package pe.edu.upc.coldchain.identity.dto;

import java.util.Set;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import pe.edu.upc.coldchain.identity.model.Role;

public record UpdatePermissionsRequest(
        Role role,
        @NotNull(message = "La lista de puntos permitidos es obligatoria")
        Set<UUID> allowedPointIds) {
}
