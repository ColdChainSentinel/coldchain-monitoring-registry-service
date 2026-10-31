# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP07 @HU26
Característica: Estado de la unidad asignada

  Antecedentes:
    Dado el conductor "conductor01" con la unidad asignada "UNI-07"
    Y "UNI-07" con rango de 2 a 8 grados Celsius y el dispositivo "SEN-07"

  @CA01
  Escenario: Ver temperaturas, límites y fechas de medición
    Cuando selecciona "UNI-07"
    Entonces ve las últimas temperaturas, los límites de 2 a 8 grados Celsius y las fechas y horas de medición

  @CA02
  Escenario: Identificar el dispositivo afectado por una desviación
    Dado una alerta activa de "SEN-07"
    Cuando selecciona "UNI-07"
    Entonces la vista identifica a "SEN-07" como afectado
    Y permite abrir la alerta

  @CA03
  Esquema del escenario: No presentar como actual una lectura con <condición>
    Dado que "SEN-07" presenta la condición "<condición>"
    Cuando selecciona "UNI-07"
    Entonces se indica "<condición>"
    Y la última temperatura no se presenta como lectura actual
    Ejemplos:
      | condición             |
      | Datos desactualizados |
      | Sin comunicación      |
