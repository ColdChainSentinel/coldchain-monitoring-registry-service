# language: es
# Diseño (Roadmap Post-MVP): requiere identificador de medición del dispositivo.
@EP04 @HU14 @pendiente
Característica: Control de mediciones duplicadas y fuera de orden
  Como supervisor quiero que el sistema controle mediciones duplicadas y recibidas fuera
  de orden para mantener un historial consistente.

  @CA01
  Escenario: Ignorar el reenvío de una medición ya procesada
    Dado que se procesó la medición "DEV-7/001" de 9 °C en "CAM-01"
    Cuando el dispositivo reenvía la medición "DEV-7/001" con el mismo contenido
    Entonces no se crea una segunda medición
    Y no se crea una segunda alerta

  @CA02
  Escenario: Incorporar una medición atrasada sin cambiar la lectura más reciente
    Dado las siguientes mediciones de "CAM-01":
      | id        | temperatura | fecha de medición    |
      | DEV-7/010 | 5           | 2026-11-26T10:10:00Z |
    Cuando llega la medición atrasada:
      | id        | temperatura | fecha de medición    |
      | DEV-7/009 | 6           | 2026-11-26T10:05:00Z |
    Entonces el historial contiene 2 mediciones ordenadas por fecha de medición
    Y el dashboard sigue mostrando 5 °C como temperatura más reciente

  @CA03
  Escenario: Registrar un conflicto cuando el contenido cambia
    Dado que se procesó la medición "DEV-7/001" de 9 °C en "CAM-01"
    Cuando llega la medición "DEV-7/001" con 12 °C
    Entonces se registra un conflicto de medición
    Y la medición original de 9 °C no se modifica
