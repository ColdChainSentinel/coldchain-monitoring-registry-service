# language: es
# Diseño (Roadmap Post-MVP).
@EP06 @HU20 @pendiente
Característica: Agrupación de mediciones de una misma desviación
  Como responsable de operaciones quiero agrupar las mediciones de una misma desviación
  para evitar alertas repetidas por un evento continuo.

  Antecedentes:
    Dado el punto "CAM-01" con un rango térmico de 2 a 8 °C

  @CA01 @CA02
  Escenario: Mediciones consecutivas fuera de rango actualizan el mismo evento
    Cuando se registran las mediciones:
      | temperatura | fecha de medición    |
      | 9           | 2026-11-26T10:00:00Z |
      | 11          | 2026-11-26T10:01:00Z |
      | 10          | 2026-11-26T10:02:00Z |
    Entonces existe 1 evento de desviación activo en "CAM-01"
    Y el evento muestra:
      | inicio               | última medición      | mínima | máxima |
      | 2026-11-26T10:00:00Z | 2026-11-26T10:02:00Z | 9      | 11     |

  @CA03
  Escenario: Una nueva desviación tras la recuperación crea otro evento
    Dado un evento de desviación recuperado en "CAM-01"
    Cuando se registra una medición de 12 °C
    Entonces existen 2 eventos de desviación en "CAM-01"
