# language: es
@EP06 @HU19 @integracion @alcance_parcial
Característica: Evaluación de desviaciones y fallos de integración
  Las reglas son externas por punto. El MVP aún no expone el dispositivo de la alerta.
  Los valores 2 y 8 son datos de prueba, no límites universales de conservación.

  @CA01 @CA02
  Esquema del escenario: Evaluar límites inclusivos y desviaciones
    Dado un punto de monitoreo con regla térmica de 2 a 8 grados Celsius
    Cuando registro una medición de <temperatura> grados Celsius
    Entonces Telemetry responde con estado 201
    Y su evaluación queda en estado "COMPLETED"
    Y existen <cantidad> alertas de tipo "<tipo>" asociadas a la medición
    Ejemplos:
      | temperatura | cantidad | tipo             |
      | 5           | 0        | NINGUNA          |
      | 2           | 0        | NINGUNA          |
      | 8           | 0        | NINGUNA          |
      | 1           | 1        | LOW_TEMPERATURE  |
      | 9           | 1        | HIGH_TEMPERATURE |

  @CA03
  Escenario: Conservar una medición sin evaluar cuando falta una regla
    Dado un punto existente sin regla térmica
    Cuando registro una medición de 9 grados Celsius
    Entonces Telemetry responde con estado 201
    Y su evaluación queda en estado "RULE_NOT_CONFIGURED"
    Y no hay alertas asociadas a la medición

  @contrato @idempotencia
  Escenario: Repetir una evaluación sin duplicar mediciones ni alertas
    Dado un punto de monitoreo con regla térmica de 2 a 8 grados Celsius
    Cuando registro una medición de 9 grados Celsius
    Y repito dos veces la evaluación con los mismos datos
    Entonces ambas evaluaciones devuelven el mismo resultado y la misma alerta
    Y existen 1 alertas de tipo "HIGH_TEMPERATURE" asociadas a la medición
    Y solo se añadió una medición al punto

  @contrato @idempotencia
  Escenario: Rechazar la reutilización del identificador con otros datos
    Dado un punto de monitoreo con regla térmica de 2 a 8 grados Celsius
    Cuando registro una medición de 9 grados Celsius
    Y intento evaluar la misma medición con temperatura 10
    Entonces Alerts responde con estado 409
    Y existen 1 alertas de tipo "HIGH_TEMPERATURE" asociadas a la medición
    Y solo se añadió una medición al punto

  @contrato @resiliencia
  Escenario: Conservar la medición pendiente durante una caída de Alerts
    Dado un punto de monitoreo con regla térmica de 2 a 8 grados Celsius
    Y Alerts deja de estar disponible
    Cuando registro una medición de 9 grados Celsius
    Entonces Telemetry responde con estado 201
    Y su evaluación queda en estado "PENDING"
    Cuando Alerts vuelve a estar disponible y reevalúo la medición guardada
    Entonces existen 1 alertas de tipo "HIGH_TEMPERATURE" asociadas a la medición
    Y la consulta de Telemetry conserva el estado "PENDING"
    Y solo se añadió una medición al punto
