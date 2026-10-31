# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP09 @HU36
Característica: Registro de cambios (auditoría)

  @CA01
  Esquema del escenario: Auditar el cambio de <entidad>
    Cuando "supervisor01" modifica <entidad> en "2026-10-01T10:00:00Z"
    Entonces la auditoría registra autor, fecha y hora, operación y valores anteriores y nuevos
    Ejemplos:
      | entidad                         |
      | un rango                        |
      | la asociación de un dispositivo |
      | los permisos de un usuario      |
      | el estado de un incidente       |

  @CA02
  Esquema del escenario: Filtrar la auditoría por <filtro>
    Dado un supervisor autorizado
    Cuando filtra el registro por <filtro>
    Entonces se muestran los eventos coincidentes en orden cronológico
    Ejemplos:
      | filtro           |
      | usuario          |
      | período          |
      | entidad afectada |

  @CA03
  Escenario: La auditoría es de solo lectura para usuarios operativos
    Dado un usuario operativo que consulta la auditoría
    Cuando intenta modificar o eliminar un registro mediante la plataforma
    Entonces la operación se rechaza
    Y el registro permanece sin cambios
