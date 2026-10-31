# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP05 @HU17
Característica: Detalle de un punto

  Antecedentes:
    Dado un responsable de operaciones autenticado con permisos sobre "ALM-01"
    Y el punto "ALM-01" con los dispositivos "SEN-01" y "SEN-03" y un rango de 2 a 8 grados Celsius

  @CA01
  Escenario: Ver dispositivos, últimas mediciones, rangos y estados
    Cuando selecciona el punto "ALM-01"
    Entonces ve "SEN-01" y "SEN-03" con sus últimas mediciones y estados
    Y ve el rango aplicable de 2 a 8 grados Celsius

  @CA02
  Escenario: Acceder a las alertas e incidentes del punto
    Dado una alerta y un incidente asociados a "ALM-01"
    Cuando selecciona el punto "ALM-01"
    Entonces puede abrir el registro de la alerta y el del incidente desde el detalle

  @CA03
  Escenario: Mostrar los últimos datos y la hora de actualización
    Dado que el detalle de "ALM-01" está abierto
    Y que se almacenó una nueva medición de "SEN-01"
    Cuando actualiza la vista
    Entonces se muestra la nueva medición
    Y se muestra la hora de actualización de la consulta
