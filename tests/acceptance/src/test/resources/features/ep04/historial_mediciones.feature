# language: es
@EP04 @HU14 @HU18 @integracion @alcance_parcial
Característica: Historial de mediciones ordenado por fecha de medición
  El MVP consulta por punto, sin filtro de período ni dashboard. La medición más
  reciente se interpreta como la última del historial ordenado por fecha de medición.

  @HU14 @CA02 @HU18 @CA01
  Escenario: Incorporar una medición retrasada sin desplazar a la más reciente
    Dado un punto nuevo sin mediciones
    Cuando registro las siguientes mediciones en este orden:
      | temperature | timestamp            |
      | 4.5         | 2026-10-01T12:10:00Z |
      | 5.5         | 2026-10-01T12:20:00Z |
      | 3.5         | 2026-10-01T12:00:00Z |
    Entonces el historial del punto contiene 3 mediciones ordenadas por fecha de medición:
      | temperature | timestamp            |
      | 3.5         | 2026-10-01T12:00:00Z |
      | 4.5         | 2026-10-01T12:10:00Z |
      | 5.5         | 2026-10-01T12:20:00Z |
    Y la medición más reciente del punto tiene temperatura 5,5

  @HU18 @CA01
  Escenario: Consultar únicamente las mediciones del punto seleccionado
    Dado un punto nuevo sin mediciones
    Y otro punto nuevo con una medición de 6 grados Celsius
    Cuando registro las siguientes mediciones en este orden:
      | temperature | timestamp            |
      | 4           | 2026-10-01T12:00:00Z |
    Entonces el historial del punto contiene 1 mediciones ordenadas por fecha de medición:
      | temperature | timestamp            |
      | 4           | 2026-10-01T12:00:00Z |

  @HU18 @CA03 @contrato
  Escenario: Consultar un punto sin mediciones devuelve un historial vacío
    Dado un punto nuevo sin mediciones
    Entonces el historial del punto está vacío
