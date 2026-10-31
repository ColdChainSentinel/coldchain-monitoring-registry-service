# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP05 @HU16
Característica: Dashboard general

  Antecedentes:
    Dado un supervisor autenticado con ámbito sobre los puntos "ALM-01" y "ALM-02"
    Y el punto "ALM-03" fuera de su ámbito

  @CA01
  Escenario: Ver los puntos del ámbito con su última temperatura por dispositivo
    Dado que "SEN-01" de "ALM-01" registró 5 grados en "2026-10-01T12:00:00Z"
    Cuando abre el dashboard
    Entonces ve "ALM-01" y "ALM-02", pero no "ALM-03"
    Y para "SEN-01" ve 5 grados, la fecha y hora "2026-10-01T12:00:00Z" y su estado de monitoreo

  @CA02
  Esquema del escenario: Mostrar expresamente la condición "<condición>"
    Dado el dispositivo "SEN-02" de "ALM-02" <situación>
    Cuando abre el dashboard
    Entonces "SEN-02" muestra la condición "<condición>"
    Y no se representa como normal
    Ejemplos:
      | situación                            | condición             |
      | sin mediciones                       | Sin mediciones        |
      | en un punto sin rango configurado    | Sin rango             |
      | con su última medición desactualizada | Datos desactualizados |

  @CA03
  Escenario: Señalar el dispositivo con problemas en un punto con varios
    Dado el punto "ALM-01" con "SEN-01" dentro del rango y "SEN-03" fuera del rango
    Cuando abre el dashboard
    Entonces el resumen de "ALM-01" indica que requiere atención
    Y permite identificar a "SEN-03" como el dispositivo fuera del rango
