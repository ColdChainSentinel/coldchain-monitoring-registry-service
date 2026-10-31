# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP08 @HU28
Característica: Registro manual de incidentes

  Antecedentes:
    Dado el responsable de almacén "almacen01" autenticado con permisos sobre "ALM-01"

  @CA01
  Escenario: Crear un incidente abierto
    Cuando registra un incidente en "ALM-01" con la descripción "Corte de energía" y fecha del hecho "2026-10-01T08:00:00Z"
    Entonces se crea un incidente con identificador único y estado "Abierto"

  @CA02
  Escenario: Diferenciar la fecha de creación de la fecha del hecho
    Cuando registra en "2026-10-01T09:30:00Z" un incidente con fecha del hecho "2026-10-01T08:00:00Z"
    Entonces el incidente registra al autor "almacen01" y la creación en "2026-10-01T09:30:00Z"
    Y conserva la fecha del hecho "2026-10-01T08:00:00Z"

  @CA03
  Esquema del escenario: Rechazar un incidente sin <campo>
    Cuando registra un incidente sin <campo>
    Entonces se indica el campo pendiente "<campo>"
    Y no se crea el incidente
    Ejemplos:
      | campo                 |
      | punto                 |
      | descripción           |
      | fecha y hora del hecho |
