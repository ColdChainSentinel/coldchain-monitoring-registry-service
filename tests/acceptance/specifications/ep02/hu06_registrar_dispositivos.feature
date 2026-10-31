# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP02 @HU06
Característica: Registro de dispositivos de temperatura

  Antecedentes:
    Dado personal técnico autenticado
    Y el punto activo "ALM-01"

  @CA01
  Escenario: Registrar un dispositivo en un punto activo
    Cuando registra el dispositivo "SEN-01" en el punto "ALM-01"
    Entonces el dispositivo se crea con su asociación al punto "ALM-01"

  @CA01
  Escenario: Rechazar la asociación a un punto inactivo
    Dado el punto inactivo "ALM-09"
    Cuando registra el dispositivo "SEN-02" en el punto "ALM-09"
    Entonces se rechaza el registro porque el punto no está activo

  @CA02
  Escenario: Impedir un identificador de dispositivo duplicado
    Dado el dispositivo existente "SEN-01"
    Cuando registra otro dispositivo con el identificador "SEN-01"
    Entonces se impide la duplicación
    Y el dispositivo original no se modifica

  @CA03
  Escenario: Consultar identificador, estado y punto asociado
    Dado el dispositivo activo "SEN-01" asociado al punto "ALM-01"
    Cuando consulta el dispositivo
    Entonces ve el identificador "SEN-01", el estado "Activo" y el punto "ALM-01"
