package pe.edu.upc.coldchain.identity.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import pe.edu.upc.coldchain.identity.model.Role;

public record LoginResponse(
        String token,
        UUID userId,
        String username,
        String fullName,
        Role role,
        Set<UUID> allowedPointIds,
        Instant expiresAt,
        String message) {
}
