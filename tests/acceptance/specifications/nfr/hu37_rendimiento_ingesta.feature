# language: es
# Automatizado: tests/performance/ingestion-load.js (k6, perfiles load y stress).
@NFR @HU37 @rendimiento @automatizado
Característica: Rendimiento de la ingesta de mediciones
  Como responsable de operaciones quiero que la plataforma registre las lecturas con
  baja latencia aun con muchos dispositivos activos, para que las alertas lleguen a tiempo.

  Escenario: Carga esperada dentro del SLO de latencia
    Dado el ambiente "coldchain-prod" con un punto y un rango térmico de 2 a 8 °C
    Cuando 20 usuarios virtuales envían mediciones sin pausa durante 2 minutos
    Entonces se cumplen los umbrales:
      | métrica                       | umbral   |
      | p95 de POST /api/measurements | < 500 ms |
      | tasa de errores               | < 1 %    |
      | evaluaciones COMPLETED        | ≥ 99 %   |

  Escenario: Estrés con autoescalado horizontal
    Cuando la carga sube progresivamente hasta 60 usuarios virtuales
    Entonces el HPA de "telemetry-ingestion" escala por encima de 1 réplica
    Y la tasa de errores se mantiene por debajo de 1 %
    Y al terminar la carga las réplicas vuelven al mínimo
