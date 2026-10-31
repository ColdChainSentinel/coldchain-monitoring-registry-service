# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP06 @HU24
Característica: Reconocimiento de una alerta

  Antecedentes:
    Dado el responsable de operaciones "operador01" autenticado
    Y una alerta activa sin reconocer

  @CA01
  Escenario: Registrar identidad y fecha del reconocimiento
    Cuando reconoce la alerta en "2026-10-01T12:05:00Z"
    Entonces la alerta registra a "operador01" y la fecha y hora "2026-10-01T12:05:00Z"

  @CA02
  Escenario: No sobrescribir un reconocimiento existente
    Dado que "operador01" reconoció la alerta en "2026-10-01T12:05:00Z"
    Cuando "operador02" intenta reconocerla nuevamente
    Entonces se muestra que "operador01" la reconoció en "2026-10-01T12:05:00Z"
    Y el registro original no se sobrescribe

  @CA03
  Escenario: Reconocer no resuelve la alerta
    Cuando reconoce la alerta
    Entonces la condición térmica continúa "Activa"
