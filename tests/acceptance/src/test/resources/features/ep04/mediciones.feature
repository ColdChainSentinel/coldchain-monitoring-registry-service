# language: es
@EP04 @HU12 @integracion @alcance_parcial
Característica: Recepción y consulta de mediciones por punto
  El MVP recibe mediciones por UUID de punto; aún no identifica dispositivos,
  no valida su activación ni expone una fecha de recepción.

  @CA01 @CA03
  Escenario: Almacenar y consultar una medición válida
    Dado un punto de monitoreo con regla térmica de 2 a 8 grados Celsius
    Cuando envío la siguiente medición para el punto:
      | temperature | timestamp                 |
      | 5           | 2026-10-01T07:00:00-05:00 |
    Entonces Telemetry responde con estado 201
    Y la medición queda consultable con temperatura 5 y fecha UTC "2026-10-01T12:00:00Z"
    Y su evaluación queda en estado "COMPLETED"
    Y no hay alertas asociadas a la medición

  @contrato
  Escenario: Rechazar campos obligatorios ausentes
    Dado un punto de monitoreo con regla térmica de 2 a 8 grados Celsius
    Cuando envío una medición sin datos
    Entonces Telemetry responde con estado 400
    Y se informan los campos obligatorios de la medición:
      | campo             |
      | monitoringPointId |
      | temperature       |
      | timestamp         |
    Y no se añade una medición al punto

  @contrato
  Escenario: Rechazar una medición para un punto inexistente
    Dado un identificador de punto que no existe en Registry
    Cuando registro una medición de 5 grados Celsius
    Entonces Telemetry responde con estado 404
    Y el punto inexistente no tiene mediciones
