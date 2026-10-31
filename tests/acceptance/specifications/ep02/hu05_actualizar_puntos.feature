# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP02 @HU05
Característica: Actualización de puntos de monitoreo

  Antecedentes:
    Dado personal técnico autenticado
    Y el punto activo "ALM-01" llamado "Almacén central"

  @CA01
  Escenario: Modificar datos descriptivos conservando el identificador
    Cuando cambia el nombre del punto a "Almacén central norte"
    Entonces el punto conserva su identificador
    Y la consulta muestra el nombre "Almacén central norte"

  @CA02
  Escenario: Exigir reasignar o desactivar dispositivos antes de desactivar
    Dado el dispositivo activo "SEN-01" asociado al punto
    Cuando intenta desactivar el punto
    Entonces se rechaza la desactivación
    Y se indica que debe reasignar o desactivar "SEN-01" previamente

  @CA03
  Escenario: Desactivar un punto sin perder sus históricos
    Dado que el punto no tiene dispositivos activos y tiene mediciones anteriores
    Cuando desactiva el punto
    Entonces no admite nuevas asociaciones de dispositivos
    Y sus mediciones, alertas e incidentes anteriores siguen consultables
