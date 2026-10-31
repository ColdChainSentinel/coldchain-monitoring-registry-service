# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP04 @HU14
Característica: Duplicados en la ingesta (complemento pendiente)
  Alerts ya es idempotente por measurementId (features/ep06/desviaciones.feature) y
  el historial ordena mediciones retrasadas (features/ep04/historial_mediciones.feature).
  Telemetry aún no recibe un identificador de medición del dispositivo: cada envío crea otra medición.

  @CA01
  Escenario: No crear una segunda medición ni alerta por el mismo registro
    Dado el dispositivo "SEN-01" en un punto con rango de 2 a 8 grados Celsius
    Y una medición "M-001" de 9 grados ya procesada
    Cuando se recibe nuevamente el registro "M-001" con el mismo contenido
    Entonces existe una sola medición "M-001"
    Y existe una sola alerta por ese registro

  @CA02
  Escenario: No reemplazar la temperatura más reciente del dashboard
    Dado que la última medición de "SEN-01" es de 5 grados tomada en "2026-10-01T12:20:00Z"
    Cuando llega una medición válida de 3 grados tomada en "2026-10-01T12:00:00Z"
    Entonces se incorpora al historial
    Y el dashboard sigue mostrando 5 grados como temperatura más reciente

  @CA03
  Escenario: Registrar un conflicto sin sobrescribir el original
    Dado una medición "M-001" de 9 grados ya procesada
    Cuando se recibe el registro "M-001" con temperatura 10
    Entonces se registra un conflicto para "M-001"
    Y la medición conserva la temperatura 9
