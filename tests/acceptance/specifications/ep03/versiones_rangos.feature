# language: es
# Diseño de 2.6. Sin implementación ni step definitions; no se ejecuta en HU-04.
@pendiente @no_implementado
@EP03 @HU10
Característica: Trazabilidad de versiones de rangos
  Antecedentes:
    Dado un supervisor autenticado con permisos sobre el punto "ALM-01"
    Y una versión "V1" con mínimo 2 y máximo 8 vigente desde el instante "2026-10-01T00:00:00Z"

  @CA01
  Escenario: Conservar la versión anterior
    Cuando el supervisor cambia el rango a mínimo 3 y máximo 7 en el instante "2026-10-02T00:00:00Z"
    Entonces se conserva la versión "V1" con sus límites originales
    Y se registra la versión "V2" con mínimo 3 y máximo 7
    Y la nueva versión conserva el autor y la fecha y hora de vigencia "2026-10-02T00:00:00Z"

  @CA02
  Escenario: Evaluar una medición retrasada con su rango histórico
    Dado que la versión "V2" está vigente desde "2026-10-02T00:00:00Z"
    Y una medición de 7.5 grados Celsius tomada en "2026-10-01T12:00:00Z"
    Cuando la medición se recibe después de "2026-10-02T00:00:00Z"
    Entonces se evalúa utilizando la versión "V1"
    Y se considera dentro del rango de 2 a 8 grados Celsius

  @CA03
  Escenario: Conservar los límites originales de una alerta
    Dado una alerta generada con una medición de 9 grados Celsius bajo la versión "V1"
    Y que el rango actual es de 3 a 7 grados Celsius
    Cuando el supervisor consulta la alerta anterior
    Entonces se muestran los límites originales de 2 a 8 grados Celsius
