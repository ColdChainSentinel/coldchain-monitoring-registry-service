# language: es
# Automatizado: AlertApiTest y ThermalRuleApiTest (alert-management), contratos Pact
# (TelemetryConsumerPactTest / AlertsProviderPactTest) y monitoring-flow.spec.ts.
@EP06 @HU19 @automatizado
Característica: Detección de desviaciones de temperatura
  Como supervisor quiero que se detecten desviaciones de temperatura para reconocer
  condiciones que puedan comprometer los productos.

  Antecedentes:
    Dado el punto "CAM-01" con un rango térmico de 2 a 8 °C

  @CA01 @CA03
  Esquema del escenario: Evaluar una medición contra el rango aplicable
    Cuando se registra una medición de <temperatura> °C en "CAM-01"
    Entonces el resultado de la evaluación es "<resultado>"
    Y se genera una alerta de tipo "<alerta>"
    Ejemplos:
      | temperatura | resultado     | alerta           |
      | 1.99        | ALERT_CREATED | LOW_TEMPERATURE  |
      | 2           | NO_ALERT      | ninguna          |
      | 5           | NO_ALERT      | ninguna          |
      | 8           | NO_ALERT      | ninguna          |
      | 8.01        | ALERT_CREATED | HIGH_TEMPERATURE |
      | 10.5        | ALERT_CREATED | HIGH_TEMPERATURE |

  @CA02
  Escenario: La alerta conserva los datos de la desviación
    Cuando se registra una medición de 10 °C en "CAM-01"
    Entonces la alerta generada contiene:
      | campo          | valor            |
      | punto          | CAM-01           |
      | temperatura    | 10               |
      | límite mínimo  | 2                |
      | límite máximo  | 8                |
      | tipo           | HIGH_TEMPERATURE |
      | estado         | OPEN             |
    Y incluye la fecha y hora de detección

  @CA03
  Escenario: Sin rango aplicable la medición no es evaluable
    Dado el punto "CAM-02" sin rango térmico configurado
    Cuando se registra una medición de 30 °C en "CAM-02"
    Entonces la medición queda con estado "RULE_NOT_CONFIGURED"
    Y no se genera ninguna alerta de desviación
