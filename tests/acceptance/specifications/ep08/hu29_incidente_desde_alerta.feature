# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP08 @HU29
Característica: Creación o vínculo de un incidente desde una alerta

  Antecedentes:
    Dado un responsable de operaciones autenticado
    Y una alerta de "SEN-01" en el punto "ALM-01"

  @CA01
  Escenario: Conservar alerta, dispositivo y punto en el incidente
    Cuando crea un incidente desde la alerta
    Entonces el incidente conserva la referencia a la alerta, a "SEN-01" y a "ALM-01"

  @CA02
  Escenario: Mostrar el vínculo existente antes de crear otro
    Dado que la alerta ya está vinculada al incidente "INC-1"
    Cuando intenta crear un incidente desde la alerta
    Entonces se muestra el vínculo con "INC-1" antes de permitir otro registro

  @CA03
  Escenario: Vincular a un incidente existente sin modificar la medición
    Dado el incidente existente "INC-2"
    Cuando vincula la alerta a "INC-2"
    Entonces la relación es consultable desde la alerta y desde "INC-2"
    Y la medición original no se modifica
