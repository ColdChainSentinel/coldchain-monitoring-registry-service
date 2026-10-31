# language: es
@EP06 @HU19 @integracion @contrato
Característica: Contrato de evaluación de Alerts
  Alerts valida la petición y el punto antes de evaluar, y no inventa alertas.

  @CA03
  Escenario: Rechazar la evaluación directa de un punto sin regla térmica
    Dado un punto existente sin regla térmica
    Cuando solicito a Alerts evaluar una medición nueva de 9 grados Celsius
    Entonces Alerts responde con estado 422
    Y Alerts informa el código "THERMAL_RULE_NOT_CONFIGURED"
    Y no se generan alertas nuevas para el punto

  Escenario: Rechazar la evaluación de un punto inexistente
    Dado un identificador de punto que no existe en Registry
    Cuando solicito a Alerts evaluar una medición nueva de 9 grados Celsius
    Entonces Alerts responde con estado 404
    Y no se generan alertas nuevas para el punto

  Escenario: Rechazar una evaluación sin datos obligatorios
    Dado un punto de monitoreo con regla térmica de 2 a 8 grados Celsius
    Cuando solicito a Alerts evaluar una petición sin datos
    Entonces Alerts responde con estado 400
    Y Alerts identifica los campos obligatorios:
      | campo             |
      | measurementId     |
      | monitoringPointId |
      | temperature       |
      | timestamp         |
    Y no se generan alertas nuevas para el punto
