# language: es
@EP04 @HU12 @integracion @contrato @resiliencia @requiere_postgres
Característica: Ingesta ante una caída de Registry
  Sin Registry no se puede validar el punto: la medición no se guarda como operativa.
  Requiere PostgreSQL porque Registry debe conservar sus puntos tras reiniciarse.

  Escenario: Rechazar la medición mientras Registry no está disponible
    Dado un punto de monitoreo con regla térmica de 2 a 8 grados Celsius
    Y Registry deja de estar disponible
    Cuando registro una medición de 5 grados Celsius
    Entonces Telemetry responde con estado 503
    Cuando Registry vuelve a estar disponible
    Entonces no se añade una medición al punto
    Y no se generan alertas nuevas para el punto
