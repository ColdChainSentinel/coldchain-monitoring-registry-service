# Runbook de respuesta ante incidentes · ColdChain Sentinel

**Alcance:** ambientes `coldchain-dev`, `coldchain-prod` (k3s) y `coldchain` (AKS).
**Herramientas:** Grafana (`http://grafana.localhost:8088`), Prometheus, Alertmanager,
Loki (Explore en Grafana), Jaeger, Argo CD (`http://argocd.localhost:8088`), `kubectl`
con el rol `coldchain-operator`.

## Severidades y tiempos de respuesta

| Severidad | Ejemplo | Primera respuesta | Canal |
|---|---|---|---|
| critical | Servicio caído, presupuesto de error ardiendo | 15 min | Guardia (Alertmanager → receptor `guardia`) |
| warning | p95 > 500 ms, errores 5xx > 5 %, reinicios | 1 h hábil | Canal del equipo |
| info | HPA en su máximo | Próxima daily | Tablero del sprint |

## Procedimiento general

1. **Reconocer** la alerta en Alertmanager y anunciar en el canal quién la atiende.
2. **Evaluar impacto**: dashboard *ColdChain Sentinel · SLO y servicios* (disponibilidad,
   p95 de ingesta, mediciones por minuto) del ambiente afectado.
3. **Mitigar primero** (rollback, escalar, reiniciar) y **diagnosticar después**.
4. **Comunicar** el estado cada 30 min mientras la severidad sea *critical*.
5. **Cerrar** cuando la alerta se resuelva y el SLI vuelva al objetivo; registrar el
   incidente y programar el postmortem sin culpables (48 h).

## RB-01 Servicio caído

*Alerta:* `ColdChainServiceDown` (`up == 0` por 1 min).

1. `kubectl -n <ns> get pods -l app=<servicio>` → estado y reinicios.
2. `kubectl -n <ns> describe pod <pod>` → eventos (OOMKilled, ImagePullBackOff, probes).
3. Logs en Grafana Explore → Loki: `{namespace="<ns>", app="<servicio>"}`.
4. Si coincide con un despliegue reciente (Argo CD → History), ejecutar **rollback**
   (RB-06). Si es OOMKilled, aumentar `limits.memory` mediante PR.
5. Si la base no responde: `kubectl -n <ns> get pod postgres-0` y revisar el PVC.

## RB-02 Errores 5xx / presupuesto de error

*Alertas:* `ColdChainHighErrorRate`, `ColdChainErrorBudgetBurn`.

1. Panel *Respuestas por código HTTP* → identificar servicio y endpoint.
2. Jaeger → servicio afectado → trazas con error: muestra en qué salto falla
   (Telemetry → Registry → Alerts).
3. 503 en todos los servicios → revisar Identity Access (introspección de sesión).
4. Mitigar con rollback si hubo despliegue; si no, escalar el servicio:
   `kubectl -n <ns> scale deploy/<servicio> --replicas=2`.

## RB-03 Latencia alta

*Alerta:* `ColdChainHighLatency` (p95 > 500 ms por 5 min).

1. Dashboard *Kubernetes y capacidad*: CPU cerca del límite (500m) o *throttling*.
2. Verificar si el HPA ya escaló (`kubectl -n <ns> get hpa`).
3. Heap JVM cerca del límite → posible presión de GC; revisar panel *Heap JVM*.
4. Consultas lentas: trazas de Jaeger con spans de base de datos largos.

## RB-04 Reinicios frecuentes

*Alerta:* `ColdChainPodRestarting` (> 2 reinicios en 15 min).

1. `kubectl -n <ns> get pod <pod> -o jsonpath='{.status.containerStatuses[0].lastState}'`.
2. `OOMKilled` → subir memoria; `Error` → logs del contenedor anterior
   (`kubectl logs <pod> --previous`).
3. Fallas de *startupProbe* tras un despliegue → rollback (RB-06).

## RB-05 Capacidad (HPA en su máximo)

*Alerta:* `ColdChainHpaAtMaximum` (10 min en el máximo).

1. Confirmar con el panel *Réplicas del HPA* que la carga es legítima.
2. Ajustar `maxReplicas` en `base/autoscaling.yaml` mediante PR (GitOps).
3. En AKS el *cluster autoscaler* agrega un nodo hasta `max_node_count`.

## RB-06 Rollback de una versión

1. **Argo CD (inmediato):** Applications → `coldchain-prod` → *History and Rollback* →
   seleccionar la revisión sana → *Rollback*. Argo CD desactiva el auto-sync hasta
   que Git vuelva a un estado sano.
2. **Git (definitivo):** GitHub Actions → *ColdChain CD (GitOps)* → `rollback` con el SHA
   anterior (aprobación del environment `production`), o `git revert` del commit
   `chore(cd): promote ...`.
3. Verificar en el pie de página del dashboard la versión activa y la alerta resuelta.

## Escalamiento

| Nivel | Responsable | Cuándo |
|---|---|---|
| 1 | Guardia del sprint (rol operator) | Toda alerta |
| 2 | Responsable de plataforma (Azure/Kubernetes) | Sin mitigación en 30 min o falla del clúster |
| 3 | Product Owner | Impacto en clientes (pérdida de lecturas o alertas) |
