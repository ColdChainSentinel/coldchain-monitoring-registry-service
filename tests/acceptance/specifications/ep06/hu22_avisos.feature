# language: es
# Diseño pendiente. Sin implementación ni step definitions; no se ejecuta.
@pendiente @no_implementado
@EP06 @HU22
Característica: Avisos visibles de nuevas alertas

  Antecedentes:
    Dado un supervisor con ámbito sobre "ALM-01" y sin acceso a "ALM-03"

  @CA01
  Escenario: Mostrar el aviso de una alerta de su ámbito
    Cuando se genera una alerta de 9 grados en "ALM-01" en "2026-10-01T12:00:00Z"
    Entonces aparece un aviso con el punto "ALM-01", 9 grados y la fecha y hora "2026-10-01T12:00:00Z"

  @CA01
  Escenario: No avisar alertas fuera de su ámbito
    Cuando se genera una alerta en "ALM-03"
    Entonces no aparece un aviso para el supervisor

  @CA02
  Escenario: Abrir el detalle desde el aviso
    Dado un aviso de una alerta de "ALM-01"
    Cuando selecciona el aviso
    Entonces accede al detalle de esa alerta

  @CA03
  Escenario: Actualizar el aviso sin una entrada por medición
    Dado un aviso de un evento activo de "ALM-01"
    Cuando el evento recibe dos mediciones más sin convertirse en un evento nuevo
    Entonces existe un solo aviso para el evento y muestra la información actualizada
