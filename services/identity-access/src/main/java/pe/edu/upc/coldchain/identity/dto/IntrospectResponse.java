package pe.edu.upc.coldchain.identity.dto;

import java.util.Set;
import java.util.UUID;
import pe.edu.upc.coldchain.identity.model.Role;

public record IntrospectResponse(
        UUID userId,
        String username,
        String fullName,
        Role role,
        Set<UUID> allowedPointIds) {
}
