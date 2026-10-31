# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP05 @HU18
Característica: Historial de temperaturas por dispositivo, punto y período (complemento pendiente)
  El orden por fecha de medición y el filtro por punto se ejecutan en
  features/ep04/historial_mediciones.feature. Faltan dispositivo, período, unidad y rango.

  @CA01
  Escenario: Filtrar por dispositivo y período
    Dado mediciones de "SEN-01" y "SEN-03" entre "2026-10-01T00:00:00Z" y "2026-10-03T00:00:00Z"
    Cuando consulta el historial de "SEN-01" del "2026-10-01T00:00:00Z" al "2026-10-02T00:00:00Z"
    Entonces solo ve mediciones de "SEN-01" dentro de ese período, ordenadas por fecha y hora de medición

  @CA02
  Escenario: Identificar los datos de cada registro
    Cuando consulta el historial de "SEN-01"
    Entonces cada registro muestra temperatura, unidad, dispositivo, punto, fecha y hora y el rango aplicable, si existía

  @CA03
  Escenario: Rechazar un período invertido
    Cuando consulta el historial del "2026-10-02T00:00:00Z" al "2026-10-01T00:00:00Z"
    Entonces se rechaza la consulta

  @CA03
  Escenario: Informar que no existen resultados
    Cuando consulta un período válido sin mediciones
    Entonces se informa que no existen resultados
