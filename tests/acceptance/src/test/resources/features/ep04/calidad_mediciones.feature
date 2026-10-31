# language: es
@EP04 @HU13 @integracion @alcance_parcial
Característica: Rechazo de mediciones inválidas
  El MVP rechaza la medición antes de persistirla. Aún no identifica dispositivos ni
  unidades, y el motivo solo se devuelve en la respuesta: no se registra un rechazo.

  @CA01 @CA03
  Esquema del escenario: Rechazar una medición con <defecto>
    Dado un punto de monitoreo con regla térmica de 2 a 8 grados Celsius
    Cuando envío una medición para el punto con temperatura <temperatura> y fecha <fecha>
    Entonces Telemetry responde con estado 400
    Y se informa el motivo del rechazo
    Y no se añade una medición al punto
    Y no se generan alertas nuevas para el punto
    Ejemplos:
      | defecto                      | temperatura | fecha                       |
      | temperatura textual          | "nueve"     | "2026-10-01T12:00:00Z"      |
      | temperatura como cadena      | "9"         | "2026-10-01T12:00:00Z"      |
      | temperatura nula             | null        | "2026-10-01T12:00:00Z"      |
      | fecha sin zona horaria       | 9           | "2026-10-01T12:00:00"       |
      | fecha de calendario inválida | 9           | "2026-02-30T12:00:00Z"      |
      | fecha con formato no RFC3339 | 9           | "01/10/2026 12:00"          |
      | fecha numérica               | 9           | 1759320000                  |

  @CA02 @contrato
  Escenario: No incorporar datos con un identificador de punto mal formado
    Dado un punto de monitoreo con regla térmica de 2 a 8 grados Celsius
    Cuando envío una medición con el identificador de punto "SEN-01"
    Entonces Telemetry responde con estado 400
    Y no se añade una medición al punto
    Y no se generan alertas nuevas para el punto
