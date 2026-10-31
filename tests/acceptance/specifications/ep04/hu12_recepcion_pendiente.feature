# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP04 @HU12
Característica: Recepción de mediciones por dispositivo (complemento pendiente)
  La parte implementada (punto, temperatura, fecha, consulta) se ejecuta en
  features/ep04/mediciones.feature. Aquí queda lo que el MVP aún no soporta.

  @CA01
  Escenario: Almacenar dispositivo, unidad y punto asociado
    Dado el dispositivo activo "SEN-01" asociado al punto "ALM-01"
    Cuando "SEN-01" envía una medición válida de 5 "°C" tomada en "2026-10-01T12:00:00Z"
    Entonces se almacenan el identificador, la temperatura 5, la unidad "°C", la fecha de medición y el punto "ALM-01"

  @CA02
  Escenario: Registrar la fecha y hora de recepción
    Dado el dispositivo activo "SEN-01"
    Cuando se recibe en "2026-10-01T12:03:00Z" una medición tomada en "2026-10-01T12:00:00Z"
    Entonces la medición registra la fecha de recepción "2026-10-01T12:03:00Z"
    Y la fecha de medición "2026-10-01T12:00:00Z" se conserva distinta
