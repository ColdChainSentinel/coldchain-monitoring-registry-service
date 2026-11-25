package pe.edu.upc.coldchain.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDeviceRequest(
        @NotBlank(message = "El identificador del dispositivo es obligatorio")
        @Size(max = 40, message = "El identificador admite hasta 40 caracteres")
        @Pattern(regexp = "[A-Za-z0-9_-]+", message = "Use letras, números, guion o guion bajo")
        String code,
        @NotNull(message = "El punto de monitoreo es obligatorio")
        UUID monitoringPointId) {
}
