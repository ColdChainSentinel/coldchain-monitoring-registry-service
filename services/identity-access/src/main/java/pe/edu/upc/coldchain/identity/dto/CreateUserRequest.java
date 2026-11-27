package pe.edu.upc.coldchain.identity.dto;

import java.util.Set;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.edu.upc.coldchain.identity.model.Role;

public record CreateUserRequest(
        @NotBlank(message = "El identificador es obligatorio")
        @Size(min = 3, max = 60, message = "El identificador debe tener entre 3 y 60 caracteres")
        @Pattern(regexp = "[A-Za-z0-9._-]+", message = "Use letras, números, punto, guion o guion bajo")
        String username,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 100, message = "La contraseña debe tener al menos 8 caracteres")
        String password,
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 120, message = "El nombre admite hasta 120 caracteres")
        String fullName,
        @NotNull(message = "El rol es obligatorio")
        Role role,
        Set<UUID> allowedPointIds) {
}
