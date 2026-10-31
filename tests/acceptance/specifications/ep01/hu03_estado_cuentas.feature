# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP01 @HU03
Característica: Gestión del estado de las cuentas

  Antecedentes:
    Dado un administrador autenticado

  @CA01
  Escenario: Registrar una cuenta con identificador único
    Cuando registra la cuenta "conductor01" con los datos obligatorios y el rol "Conductor"
    Entonces la cuenta queda disponible con el rol "Conductor"

  @CA01
  Escenario: Rechazar un identificador de acceso repetido
    Dado una cuenta existente "conductor01"
    Cuando registra otra cuenta con el identificador "conductor01"
    Entonces se rechaza el registro por identificador duplicado

  @CA02
  Escenario: Bloquear nuevos accesos y sesiones existentes al desactivar
    Dado la cuenta "conductor01" con una sesión iniciada
    Cuando el administrador desactiva la cuenta
    Entonces un nuevo inicio de sesión de "conductor01" se rechaza
    Y la sesión existente ya no permite operaciones protegidas

  @CA03
  Escenario: Conservar la autoría tras la desactivación
    Dado que "conductor01" registró un incidente y una acción
    Cuando el administrador desactiva la cuenta
    Entonces el incidente y la acción siguen identificando a "conductor01" como autor
