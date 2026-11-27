package pe.edu.upc.coldchain.telemetry.security;

import java.util.Set;
import java.util.UUID;

public record AuthContext(
        UUID userId,
        String username,
        String role,
        Set<UUID> allowedPointIds) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean canAccessPoint(UUID pointId) {
        if (isAdmin()) {
            return true;
        }
        return allowedPointIds != null && allowedPointIds.contains(pointId);
    }
}
