# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP04 @HU13
Característica: Mediciones inválidas (complemento pendiente)
  El rechazo de temperatura y fecha inválidas se ejecuta en features/ep04/calidad_mediciones.feature.

  @CA01
  Esquema del escenario: Rechazar una medición sin <dato>
    Cuando llega una medición con <defecto>
    Entonces se rechaza como medición operativa
    Ejemplos:
      | dato               | defecto                       |
      | dispositivo        | el dispositivo ausente        |
      | unidad admitida    | la unidad "K°"                |

  @CA02
  Escenario: No incorporar datos de un dispositivo desconocido
    Cuando llega una medición válida del dispositivo no registrado "SEN-99"
    Entonces no se incorpora al historial operativo

  @CA03
  Escenario: Registrar el motivo sin alterar el monitoreo
    Dado el punto "ALM-01" con rango de 2 a 8 grados Celsius y última temperatura 5
    Cuando se rechaza una medición de 15 grados con unidad no admitida
    Entonces se registra el motivo de rechazo
    Y el dashboard sigue mostrando 5 grados para el punto
    Y no se genera una alerta de temperatura
