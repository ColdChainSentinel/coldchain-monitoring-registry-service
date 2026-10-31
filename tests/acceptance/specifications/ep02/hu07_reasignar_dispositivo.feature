# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP02 @HU07
Característica: Reasignación de un dispositivo a otro punto

  Antecedentes:
    Dado personal técnico autenticado
    Y el dispositivo "SEN-01" asociado al punto "ALM-01" desde "2026-10-01T00:00:00Z"
    Y el punto activo "ALM-02"

  @CA01
  Escenario: Registrar la nueva asociación con su vigencia
    Cuando reasigna "SEN-01" al punto "ALM-02" y confirma el cambio en "2026-10-02T00:00:00Z"
    Entonces se registra la asociación con "ALM-02" vigente desde "2026-10-02T00:00:00Z"
    Y se conserva la asociación anterior con "ALM-01"

  @CA02
  Escenario: Conservar el punto de las mediciones anteriores al cambio
    Dado una medición de "SEN-01" tomada en "2026-10-01T12:00:00Z"
    Y que "SEN-01" fue reasignado a "ALM-02" en "2026-10-02T00:00:00Z"
    Cuando consulta la medición
    Entonces pertenece al punto "ALM-01"

  @CA03
  Esquema del escenario: Vincular una medición retrasada según su fecha de medición (<caso>)
    Dado que "SEN-01" fue reasignado a "ALM-02" en "2026-10-02T00:00:00Z"
    Cuando se recibe después del cambio una medición de "SEN-01" tomada en "<fecha>"
    Entonces la medición <resultado>
    Ejemplos:
      | caso                    | fecha                | resultado                              |
      | antes del cambio        | 2026-10-01T12:00:00Z | se vincula al punto "ALM-01"           |
      | después del cambio      | 2026-10-02T12:00:00Z | se vincula al punto "ALM-02"           |
      | sin asociación vigente  | 2026-09-30T12:00:00Z | queda identificada como no asociable   |
