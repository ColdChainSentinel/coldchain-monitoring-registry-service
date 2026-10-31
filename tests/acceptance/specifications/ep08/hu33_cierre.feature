# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP08 @HU33
Característica: Cierre documentado de un incidente

  Antecedentes:
    Dado un supervisor autenticado
    Y el incidente "En atención" "INC-1" con acciones registradas

  @CA01
  Escenario: Cerrar con resultado de resolución
    Cuando cierra "INC-1" con el resultado "Producto trasladado sin pérdidas"
    Entonces "INC-1" cambia a "Cerrado"

  @CA02
  Escenario: Impedir el cierre sin resultado
    Cuando intenta cerrar "INC-1" sin resultado de resolución
    Entonces se impide el cierre
    Y se indica que falta el resultado de resolución

  @CA03
  Escenario: Exigir justificación con una desviación activa
    Dado que "INC-1" está vinculado a una alerta todavía activa
    Cuando intenta cerrarlo con resultado y sin justificación
    Entonces se exige una justificación
    Cuando lo cierra con resultado y justificación
    Entonces "INC-1" cambia a "Cerrado"
    Y la alerta térmica permanece activa

  @CA04
  Escenario: Conservar autor, fecha e historial del cierre
    Cuando cierra "INC-1" con resultado en "2026-10-01T15:00:00Z"
    Entonces se conservan el autor, la fecha y hora "2026-10-01T15:00:00Z" y el historial completo del incidente
