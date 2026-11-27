# Validación de Kubernetes local

**Fecha:** 26/11/2026 (Lima). **Código de aplicación:** `7a6a0f7` (`main`).
**Entorno:** Docker Desktop, Kubernetes v1.36.1, un nodo Ready, namespace
`coldchain-local`, PostgreSQL 16.6 con PVC de 1 GiB. Se construyeron las imágenes
desde los cuatro Dockerfiles del repositorio.

| Comprobación | Resultado observado |
|---|---|
| Render de manifiestos | 15 recursos locales y 12 recursos en la plantilla cloud. |
| Despliegue de componentes | PostgreSQL listo, PVC Bound y rollout exitoso de Registry, Telemetry, Alerts y frontend. |
| Acceso a frontend | HTTP 200 por port-forward al Service de frontend. |
| Caso integrado | Registro de punto de demostración, regla de 2–8 °C, medición de 10 °C persistida en Telemetry con evaluación COMPLETED y una alerta HIGH_TEMPERATURE en estado OPEN para esa medición. |

Resultado del script `infrastructure/kubernetes/Test-Local.ps1`:

```text
OK: K8S-629603CFAA / 5a9e6715-2054-4c44-920c-440efd3bafb2 / medición c461669a-8f6f-47b4-ba90-4927de891993 / alerta 4b2f8aa8-4d17-4948-8cf1-c71a81fd1553
```

La primera ejecución persistió la medición con estado PENDING justo después de
reiniciar Alert Management. Una ejecución posterior comprobó la ruta HTTP de
Alerts con una lectura antes de escribir y completó el flujo. El script no reintenta
la escritura de telemetría.

La prueba fue local. El clúster no ofrecía `metrics.k8s.io`, así que el HPA quedó
declarado sin prueba de escalado. El acceso usó port-forward; Ingress TLS, despliegue
cloud, promoción y rollback siguen pendientes en el ambiente del equipo.
