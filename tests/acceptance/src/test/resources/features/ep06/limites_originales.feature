# language: es
@EP03 @HU10 @integracion @alcance_parcial @requiere_postgres
Característica: Límites originales de una alerta tras cambiar la regla
  La regla externa cambia reiniciando Alerts con otra configuración. No hay API de
  versiones, autor ni vigencia: solo se verifica el snapshot de límites de la alerta.
  Requiere PostgreSQL porque las alertas deben sobrevivir al reinicio de Alerts.

  @CA03
  Escenario: Conservar los límites usados aunque la regla actual sea otra
    Dado un punto de monitoreo con regla térmica propia de 2 a 8 grados Celsius
    Y registro una medición de 9 grados Celsius
    Y existen 1 alertas de tipo "HIGH_TEMPERATURE" asociadas a la medición
    Cuando la regla térmica del punto cambia a 3 a 7 grados Celsius
    Entonces la alerta anterior conserva los límites de 2 a 8 grados Celsius
    Cuando registro una medición de 7,5 grados Celsius
    Entonces existen 1 alertas de tipo "HIGH_TEMPERATURE" asociadas a la medición
    Y la nueva alerta usa los límites de 3 a 7 grados Celsius
