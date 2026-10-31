# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP06 @HU21
Característica: Recuperación de temperatura

  Antecedentes:
    Dado una desviación activa de "SEN-01" en un punto con rango de 2 a 8 grados Celsius

  @CA01
  Esquema del escenario: Registrar la recuperación con una medición de <temperatura> grados
    Cuando llega una medición posterior de <temperatura> grados tomada en "2026-10-01T12:30:00Z"
    Entonces se registra la recuperación en "2026-10-01T12:30:00Z"
    Ejemplos:
      | temperatura |
      | 5           |
      | 8           |

  @CA02
  Escenario: No recuperar la alerta por ausencia de datos
    Cuando dejan de llegar mediciones y el dispositivo queda "Sin comunicación"
    Entonces la alerta no se considera recuperada

  @CA03
  Escenario: La recuperación no cierra el incidente ni declara apto el producto
    Dado un incidente "En atención" vinculado a la alerta
    Cuando se registra la recuperación
    Entonces el incidente permanece "En atención"
    Y no se declara que el producto está apto para su uso
