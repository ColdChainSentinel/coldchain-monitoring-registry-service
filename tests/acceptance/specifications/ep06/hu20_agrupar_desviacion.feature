# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP06 @HU20
Característica: Agrupación de mediciones de una misma desviación
  Observación: el MVP actual crea una alerta por cada medición fuera de rango; contradice CA-01.

  Antecedentes:
    Dado el dispositivo "SEN-01" en un punto con rango de 2 a 8 grados Celsius

  @CA01
  Escenario: Actualizar el mismo evento con mediciones consecutivas
    Dado una alerta activa de "SEN-01" iniciada con 9 grados en "2026-10-01T12:00:00Z"
    Cuando llegan mediciones consecutivas de 10 y 9,5 grados bajo la misma configuración
    Entonces existe un único evento activo para "SEN-01"

  @CA02
  Escenario: Consultar inicio, última medición y extremos del evento
    Dado un evento con mediciones de 9, 10 y 9,5 grados entre "2026-10-01T12:00:00Z" y "2026-10-01T12:10:00Z"
    Cuando consulta el evento
    Entonces ve inicio "2026-10-01T12:00:00Z", última medición "2026-10-01T12:10:00Z", mínima 9 y máxima 10

  @CA03
  Escenario: Crear un evento nuevo después de una recuperación
    Dado un evento de "SEN-01" recuperado con una medición de 5 grados
    Cuando llega una medición de 9 grados
    Entonces se crea un evento nuevo distinto del anterior

  @CA01
  Escenario: Un cambio de configuración no reutiliza el evento
    Dado una alerta activa de "SEN-01" bajo la versión de rango "V1"
    Y que se activa la versión de rango "V2" con límites de 3 a 7
    Cuando llega una medición de 9 grados
    Entonces la medición no se agrupa en el evento iniciado bajo "V1"
