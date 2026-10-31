# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP06 @HU23
Característica: Búsqueda y filtrado de alertas
  Observación: el MVP lista por punto en orden ascendente de creación; CA-02 exige el inverso.

  Antecedentes:
    Dado un responsable de operaciones autenticado
    Y alertas de "ALM-01" y "ALM-02", activas y recuperadas, reconocidas y sin reconocer

  @CA01
  Esquema del escenario: Filtrar alertas por <filtro>
    Cuando filtra por <filtro> con el valor "<valor>"
    Entonces solo se muestran las alertas que cumplen ese filtro
    Ejemplos:
      | filtro         | valor                                       |
      | punto          | ALM-01                                      |
      | dispositivo    | SEN-01                                      |
      | período        | 2026-10-01T00:00:00Z/2026-10-02T00:00:00Z   |
      | condición      | Recuperada                                  |
      | reconocimiento | Sin reconocer                               |

  @CA02
  Escenario: Ordenar de la detección más reciente a la más antigua
    Cuando consulta el listado sin un orden personalizado
    Entonces las alertas aparecen de la detección más reciente a la más antigua

  @CA03
  Escenario: Informar la ausencia de resultados
    Cuando aplica filtros sin coincidencias
    Entonces se muestra un mensaje de ausencia de resultados
    Y puede modificar los filtros
