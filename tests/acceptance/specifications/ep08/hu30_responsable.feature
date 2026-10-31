# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP08 @HU30
Característica: Asignación de responsable

  Antecedentes:
    Dado un supervisor autenticado
    Y el incidente abierto "INC-1" del punto "ALM-01"

  @CA01
  Escenario: Registrar un responsable válido
    Dado el usuario activo "operador01" con permisos sobre "ALM-01"
    Cuando asigna "operador01" como responsable de "INC-1"
    Entonces "operador01" queda registrado como responsable

  @CA01
  Esquema del escenario: Rechazar un responsable <caso>
    Cuando asigna como responsable a <usuario>
    Entonces se rechaza la asignación
    Ejemplos:
      | caso                     | usuario                                  |
      | inactivo                 | una cuenta inactiva                      |
      | sin permisos en el punto | un usuario sin permisos sobre "ALM-01"   |

  @CA02
  Escenario: Conservar el historial de reasignaciones
    Dado que "operador01" es responsable de "INC-1"
    Cuando reasigna "INC-1" a "operador02" en "2026-10-01T10:00:00Z"
    Entonces se conservan el responsable anterior "operador01", el nuevo "operador02", el autor y la fecha y hora

  @CA03
  Escenario: El incidente aparece en el listado del responsable
    Dado que "operador01" es responsable de "INC-1"
    Cuando "operador01" consulta sus incidentes
    Entonces "INC-1" aparece en su listado
