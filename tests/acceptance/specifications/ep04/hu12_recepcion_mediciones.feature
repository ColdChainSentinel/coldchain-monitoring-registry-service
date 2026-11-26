# language: es
# Automatizado: Playwright (tests/e2e/specs/monitoring-flow.spec.ts, paso HU-12),
# Test-Local.ps1 en Kubernetes y MeasurementApiTest (telemetry-ingestion).
@EP04 @HU12 @automatizado
Característica: Recepción y almacenamiento centralizado de mediciones
  Como supervisor quiero que las mediciones se reciban y almacenen de forma centralizada
  para contar con información trazable de la cadena de frío.

  Antecedentes:
    Dado un usuario autenticado con acceso al punto "CAM-01"
    Y el punto "CAM-01" tiene un rango térmico de 2 a 8 °C

  @CA01 @CA02
  Escenario: Almacenar una medición válida con su fecha de recepción
    Cuando el dispositivo envía la medición:
      | punto  | temperatura | fecha de medición    |
      | CAM-01 | 4.25        | 2026-11-26T10:00:00Z |
    Entonces la API responde con estado 201
    Y la medición queda guardada con los siguientes datos:
      | campo             | valor                |
      | temperatura       | 4.25                 |
      | unidad            | °C                   |
      | fecha de medición | 2026-11-26T10:00:00Z |
      | punto             | CAM-01               |
    Y se registra la fecha y hora de recepción

  @CA03
  Escenario: La medición queda disponible para su consulta
    Dado que se registró una medición de 5 °C en "CAM-01"
    Cuando consulto las mediciones del punto "CAM-01"
    Entonces el listado incluye la medición de 5 °C con estado de evaluación "COMPLETED"

  @CA01
  Esquema del escenario: Aceptar temperaturas en los extremos del rango operativo
    Cuando el dispositivo envía una medición de <temperatura> °C en "CAM-01"
    Entonces la API responde con estado 201
    Ejemplos:
      | temperatura |
      | -40         |
      | 0           |
      | 49.99       |
      | 50          |
