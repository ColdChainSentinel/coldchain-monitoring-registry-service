# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP07 @HU25
Característica: Asignación de una unidad refrigerada a un conductor

  Antecedentes:
    Dado un supervisor de transporte autenticado
    Y el conductor activo "conductor01"
    Y la unidad de transporte registrada "UNI-07"

  @CA01
  Escenario: Guardar la asignación con su vigencia
    Cuando asigna "UNI-07" a "conductor01" desde "2026-10-01T06:00:00Z"
    Entonces se guarda la asignación vigente desde "2026-10-01T06:00:00Z"

  @CA01
  Esquema del escenario: Rechazar la asignación de <caso>
    Cuando asigna <unidad> a <conductor>
    Entonces se rechaza la asignación
    Ejemplos:
      | caso                     | unidad              | conductor              |
      | un conductor inactivo    | "UNI-07"            | el conductor inactivo  |
      | un punto de almacenamiento | el punto "ALM-01" | "conductor01"          |

  @CA02
  Escenario: El conductor identifica sus unidades asignadas
    Dado que "UNI-07" está asignada a "conductor01"
    Cuando "conductor01" accede al monitoreo
    Entonces puede identificar y seleccionar "UNI-07"

  @CA03
  Escenario: Finalizar la asignación conservando el historial
    Dado que "UNI-07" está asignada a "conductor01"
    Cuando finaliza la asignación
    Entonces se conserva el historial de la asignación
    Y "conductor01" deja de acceder a "UNI-07" salvo que otros permisos lo habiliten
