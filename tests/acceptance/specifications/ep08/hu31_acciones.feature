# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP08 @HU31
Característica: Registro de acciones en un incidente

  Antecedentes:
    Dado "operador01" responsable del incidente "INC-1"

  @CA01
  Escenario: Guardar la acción con identidad y fecha
    Cuando registra la acción "Se trasladó el producto a la cámara 2" en "2026-10-01T10:15:00Z"
    Entonces la acción se guarda con "operador01" y la fecha y hora "2026-10-01T10:15:00Z"

  @CA02
  Escenario: Conservar las acciones anteriores
    Dado la acción registrada "Se revisó la puerta"
    Cuando registra la acción "Se cambió el sello"
    Entonces el incidente muestra ambas acciones con su contenido original

  @CA03
  Escenario: Rechazar una acción sin descripción
    Cuando registra una acción sin descripción
    Entonces se rechaza el registro y se solicita completar la descripción
