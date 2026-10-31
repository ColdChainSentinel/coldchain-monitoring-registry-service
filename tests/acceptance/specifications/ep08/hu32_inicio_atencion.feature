# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP08 @HU32
Característica: Inicio de la atención de un incidente

  Antecedentes:
    Dado un responsable de operaciones "operador01" autenticado

  @CA01 @CA02
  Escenario: Cambiar a "En atención" registrando la transición
    Dado el incidente "Abierto" "INC-1" con responsable asignado
    Cuando inicia su atención en "2026-10-01T10:00:00Z"
    Entonces "INC-1" cambia a "En atención"
    Y se registra el estado anterior "Abierto", el nuevo "En atención", el autor "operador01" y la fecha y hora

  @CA01
  Escenario: No iniciar la atención sin responsable
    Dado el incidente "Abierto" "INC-2" sin responsable
    Cuando intenta iniciar su atención
    Entonces se rechaza el cambio de estado

  @CA03
  Escenario: No reabrir un incidente cerrado mediante la acción de inicio
    Dado el incidente "Cerrado" "INC-3"
    Cuando intenta iniciar su atención
    Entonces se rechaza el cambio
    Y "INC-3" permanece "Cerrado"
