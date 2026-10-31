# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP08 @HU34
Característica: Consulta de incidentes y su historial

  Antecedentes:
    Dado un supervisor autenticado
    Y incidentes de distintos puntos, períodos, responsables y estados

  @CA01
  Esquema del escenario: Filtrar incidentes por <filtro>
    Cuando filtra por <filtro> con el valor "<valor>"
    Entonces solo se muestran los incidentes coincidentes
    Ejemplos:
      | filtro      | valor                                     |
      | punto       | ALM-01                                    |
      | período     | 2026-10-01T00:00:00Z/2026-10-02T00:00:00Z |
      | responsable | operador01                                |
      | estado      | En atención                               |

  @CA02
  Escenario: Ver el historial completo en orden cronológico
    Dado el incidente "INC-1" con alertas vinculadas, acciones y cambios de estado
    Cuando abre "INC-1"
    Entonces ve descripción, autor, responsable, alertas vinculadas, acciones y cambios de estado en orden cronológico

  @CA03
  Esquema del escenario: Conservar el histórico de un incidente <caso>
    Dado el incidente "INC-2" <situación>
    Cuando lo consulta
    Entonces su información histórica continúa disponible
    Ejemplos:
      | caso                    | situación                               |
      | cerrado                 | cerrado                                 |
      | de dispositivo inactivo | asociado al dispositivo inactivo "SEN-01" |
