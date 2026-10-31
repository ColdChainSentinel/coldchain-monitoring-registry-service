# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP07 @HU27
Característica: Comunicación de una incidencia desde la unidad

  Antecedentes:
    Dado el conductor "conductor01" con la unidad asignada "UNI-07"

  @CA01
  Escenario: Incorporar la unidad y la alerta seleccionada al formulario
    Dado una alerta activa de "UNI-07" seleccionada
    Cuando inicia el registro de una incidencia desde "UNI-07"
    Entonces el formulario incluye la referencia a "UNI-07" y a la alerta seleccionada

  @CA02
  Escenario: Crear el incidente visible para operaciones
    Cuando completa la descripción "Puerta mal cerrada" y la fecha del hecho "2026-10-01T10:00:00Z"
    Y envía la incidencia
    Entonces se crea el incidente
    Y queda visible para el personal de operaciones autorizado

  @CA03
  Escenario: Informar que no se envió por falta de conexión
    Dado que no hay conexión
    Cuando envía la incidencia
    Entonces se informa que no fue enviada
    Y no se muestra una confirmación de recepción
