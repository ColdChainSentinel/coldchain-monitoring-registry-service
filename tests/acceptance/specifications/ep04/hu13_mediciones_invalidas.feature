# language: es
# Automatizado: MeasurementApiTest.rejectsInvalidTemperatureWithoutPersisting,
# validation.test.ts (frontend) y monitoring-flow.spec.ts (Playwright).
@EP04 @HU13 @automatizado
Característica: Identificación de mediciones inválidas
  Como personal técnico quiero identificar mediciones inválidas para evitar que datos
  incorrectos alteren el monitoreo.

  Antecedentes:
    Dado un usuario autenticado con acceso al punto "CAM-01"

  @CA01
  Esquema del escenario: Rechazar una medición con datos inválidos
    Cuando el dispositivo envía la medición:
      | punto   | temperatura   | fecha de medición |
      | <punto> | <temperatura> | <fecha>           |
    Entonces la API responde con estado 400
    Y se identifica el campo "<campo>" con el motivo del rechazo
    Y no se guarda ninguna medición
    Ejemplos:
      | punto  | temperatura | fecha                | campo             |
      | CAM-01 | 85          | 2026-11-26T10:00:00Z | temperature       |
      | CAM-01 | -41         | 2026-11-26T10:00:00Z | temperature       |
      | CAM-01 | "3.5"       | 2026-11-26T10:00:00Z | temperature       |
      | CAM-01 | 4           | 2026-11-26T10:00:00  | timestamp         |
      | CAM-01 | 4           | 2026-02-30T10:00:00Z | timestamp         |
      |        | 4           | 2026-11-26T10:00:00Z | monitoringPointId |

  @CA02
  Escenario: Rechazar mediciones de un punto desconocido
    Cuando el dispositivo envía una medición de 4 °C para un punto que no existe
    Entonces la API responde con estado 404
    Y la medición no se incorpora al historial

  @CA03
  Escenario: Una medición rechazada no genera alertas
    Dado el punto "CAM-01" con un rango térmico de 2 a 8 °C
    Cuando el dispositivo envía una medición de 120 °C en "CAM-01"
    Entonces la API responde con estado 400
    Y el punto "CAM-01" no tiene alertas nuevas
