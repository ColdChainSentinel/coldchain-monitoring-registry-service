# language: es
# Especificación de HU-04; no se ejecuta mediante Cucumber en esta entrega.
Característica: Registro de puntos de monitoreo
  Escenario: CA-01 Registrar un punto válido
    Dado que no existe el código "CAM-01"
    Cuando registro código "CAM-01", nombre "Cámara 1" y tipo "ALMACENAMIENTO"
    Entonces se crea el punto y aparece en el listado

  Escenario: CA-02 Rechazar un duplicado
    Dado que existe el código "CAM-01"
    Cuando intento registrar otro punto con el mismo código
    Entonces se informa la duplicidad y no se crea un registro adicional

  Escenario: CA-03 Rechazar datos obligatorios ausentes
    Cuando envío una solicitud sin código, nombre ni tipo
    Entonces se identifican los campos y no se guarda ningún punto
