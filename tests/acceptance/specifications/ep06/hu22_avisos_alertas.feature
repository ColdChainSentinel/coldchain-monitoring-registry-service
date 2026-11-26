# language: es
# Diseño (Roadmap Post-MVP). El objetivo de aviso < 60 s se mide con el SLO-4.
@EP06 @HU22 @pendiente
Característica: Avisos visibles de nuevas alertas
  Como supervisor, responsable de almacén o conductor quiero recibir avisos visibles de
  nuevas alertas para reconocer que debo revisar una desviación.

  @CA01
  Escenario: Mostrar un aviso a los usuarios con acceso al punto
    Dado los siguientes usuarios:
      | usuario   | rol        | puntos permitidos |
      | sup-norte | SUPERVISOR | CAM-01            |
      | sup-sur   | SUPERVISOR | CAM-09            |
    Cuando se genera una alerta de 10 °C en "CAM-01"
    Entonces "sup-norte" ve un aviso con el punto, la temperatura y la fecha y hora
    Y "sup-sur" no ve el aviso

  @CA02
  Escenario: Abrir el detalle desde el aviso
    Dado un aviso de alerta visible para "sup-norte"
    Cuando selecciona el aviso
    Entonces se muestra el detalle de la alerta correspondiente

  @CA03
  Escenario: Nuevas mediciones del mismo evento no duplican el aviso
    Dado un aviso de alerta activo en "CAM-01"
    Cuando se registran 3 mediciones más fuera de rango en el mismo evento
    Entonces existe un único aviso para el evento
    Y el aviso muestra la última temperatura registrada
