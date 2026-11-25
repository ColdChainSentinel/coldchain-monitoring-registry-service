package pe.edu.upc.coldchain.identity.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import pe.edu.upc.coldchain.identity.model.Role;
import pe.edu.upc.coldchain.identity.model.UserAccount;

public record UserResponse(
        UUID id,
        String username,
        String fullName,
        Role role,
        boolean active,
        Set<UUID> allowedPointIds,
        Instant createdAt) {

    public static UserResponse from(UserAccount user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.isActive(),
                Set.copyOf(user.getAllowedPointIds()),
                user.getCreatedAt());
    }
}
