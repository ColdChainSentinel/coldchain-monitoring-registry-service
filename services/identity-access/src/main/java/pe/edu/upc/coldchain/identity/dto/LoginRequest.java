package pe.edu.upc.coldchain.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El identificador es obligatorio")
        @Size(max = 60)
        String username,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(max = 100)
        String password) {
}
