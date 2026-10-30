package pe.edu.upc.coldchain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.edu.upc.coldchain.model.PointType;

public record CreatePointRequest(
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 40, message = "El código admite hasta 40 caracteres")
        @Pattern(regexp = "[A-Za-z0-9_-]+", message = "Use letras, números, guion o guion bajo") String code,
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre admite hasta 120 caracteres") String name,
        @NotNull(message = "El tipo es obligatorio") PointType type) { }
