# SLIs y SLOs de ColdChain Sentinel

Ventana de evaluación: 30 días móviles. Fuente: métricas `http_server_requests_seconds_*`
de Micrometer recolectadas por Prometheus cada 15 s (reglas en
`infrastructure/kubernetes/observability/config/slo-rules.yml`).

| # | SLI | Definición (PromQL) | SLO | Presupuesto de error (30 días) | Justificación |
|---|---|---|---|---|---|
| SLO-1 | Disponibilidad de las APIs | `coldchain:availability:ratio_rate5m` = 1 − respuestas 5xx / respuestas totales (sin `/actuator`) | ≥ 99,5 % | 0,5 % de las solicitudes (~3 h 36 min equivalentes) | Objetivo SMART del capítulo I (disponibilidad cloud 99,5 %). |
| SLO-2 | Latencia de la ingesta | `coldchain:ingest_fast:ratio_rate5m` = proporción de `POST /api/measurements` respondidas en < 500 ms | ≥ 95 % | 5 % de las lecturas | La lectura atraviesa 3 servicios y 3 bases; 500 ms deja margen para el objetivo de alerta < 60 s. |
| SLO-3 | Latencia general (p95) | `coldchain:latency_p95:seconds_5m` por servicio | p95 < 500 ms | — (alerta a los 5 min) | Respuesta fluida del dashboard. |
| SLO-4 | Evaluación térmica | `evaluation_completed` (k6) / estado `COMPLETED` en Telemetry | ≥ 99 % de lecturas evaluadas | 1 % | Una lectura sin evaluar puede ocultar una desviación (HU-19). |

## Política de presupuesto de error

| Consumo del presupuesto | Acción |
|---|---|
| < 50 % | Se libera con normalidad (promoción dev → prod). |
| 50–100 % | Solo cambios de corrección; revisión de causas en la retrospectiva. |
| > 100 % | Congelamiento de funcionalidades nuevas hasta recuperar el SLO. |

La alerta `ColdChainErrorBudgetBurn` dispara cuando el consumo es 14,4 veces la tasa
permitida (agotaría el presupuesto mensual en ~2 días), siguiendo el esquema de *burn
rate* multiventana de Google SRE.
