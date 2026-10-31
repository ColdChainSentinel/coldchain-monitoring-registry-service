# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP02 @HU08
Característica: Desactivación de dispositivos retirados

  Antecedentes:
    Dado personal técnico autenticado
    Y el dispositivo activo "SEN-01" con mediciones y alertas anteriores

  @CA01
  Escenario: Registrar la desactivación
    Cuando desactiva el dispositivo "SEN-01"
    Entonces se registra el cambio con su autor y fecha y hora
    Y el dispositivo se identifica como inactivo

  @CA02
  Escenario: Rechazar datos de un dispositivo inactivo
    Dado que "SEN-01" está inactivo
    Cuando llega una medición de "SEN-01"
    Entonces no se actualiza el estado operativo del punto
    Y se registra el motivo de rechazo "Dispositivo inactivo"
    Y no se genera una alerta de temperatura

  @CA03
  Escenario: Consultar el histórico de un dispositivo inactivo
    Dado que "SEN-01" está inactivo
    Cuando consulta el dispositivo
    Entonces sus mediciones y alertas anteriores siguen disponibles
