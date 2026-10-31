# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP06 @HU19
Característica: Desviaciones con dispositivo (complemento pendiente)
  La detección, los límites y la falta de regla se ejecutan en features/ep06/desviaciones.feature.

  @CA02
  Escenario: Incluir el dispositivo en la alerta
    Dado el dispositivo "SEN-01" en un punto con rango de 2 a 8 grados Celsius
    Cuando "SEN-01" envía una medición de 9 grados
    Entonces la alerta incluye el dispositivo "SEN-01", el punto, la temperatura, los límites y la fecha y hora de detección
