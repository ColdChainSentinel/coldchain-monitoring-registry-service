# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP04 @HU15
Característica: Pérdida y recuperación de comunicación
  Usar un reloj controlado; no esperar minutos reales.

  Antecedentes:
    Dado el dispositivo activo "SEN-01" con intervalo máximo de 5 minutos
    Y su última medición válida tomada y recibida en "2026-10-01T12:00:00Z"

  @CA01
  Escenario: Mostrar "Sin comunicación" con la fecha de la última medición
    Cuando el reloj marca "2026-10-01T12:06:00Z" sin recepciones válidas
    Entonces el dispositivo se muestra "Sin comunicación"
    Y se conserva visible la última medición de "2026-10-01T12:00:00Z"

  @CA02
  Escenario: Distinguir datos desactualizados de una lectura actual
    Cuando en "2026-10-01T12:04:00Z" se recibe una medición tomada en "2026-10-01T11:30:00Z"
    Entonces el dispositivo se muestra "Datos desactualizados"
    Y la temperatura no se presenta como lectura actual

  @CA03
  Escenario: Registrar la recuperación de la comunicación
    Dado que el dispositivo está "Sin comunicación" desde "2026-10-01T12:05:00Z"
    Cuando en "2026-10-01T12:10:00Z" se recibe una medición válida tomada en "2026-10-01T12:10:00Z"
    Entonces se restablece el estado de comunicación
    Y se registra la recuperación en "2026-10-01T12:10:00Z"

  @CA03
  Escenario: La recuperación de comunicación no recupera una alerta térmica
    Dado una alerta activa de "SEN-01" por desviación alta
    Y que el dispositivo está "Sin comunicación"
    Cuando vuelve a recibirse una medición fuera del rango
    Entonces se restablece la comunicación
    Y la alerta térmica continúa activa
