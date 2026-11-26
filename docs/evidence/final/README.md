# Evidencia de la entrega final (Capítulo IV)

Ejecutada el 26/11/2026 sobre un clúster k3s v1.31.4 en Docker (1 nodo, 6 vCPU, 12 GiB),
rama `release/final-tb4`. Las horas de `cd-events.log` están en UTC.

| Carpeta / archivo | Contenido | Sección |
|---|---|---|
| `img/` | Capturas de la aplicación, Grafana, Prometheus, Alertmanager, Jaeger, Argo CD, Lighthouse, diagramas y salidas de terminal | 4.1–4.6 |
| `k8s/` | Salidas de `kubectl` (recursos, pods, RBAC, NetworkPolicy) | 4.1, 4.5 |
| `cd-events.log` | Eventos de despliegue, promoción, falla y rollback | 4.2, 4.6 |
| `rollback-failure.txt`, `rollback-probe.log` | Release defectuoso y sonda de disponibilidad (1 solicitud/s) | 4.2 |
| `reports/e2e/` | Playwright (JUnit, JSON) y axe después de las correcciones | 4.4.1, 4.4.5 |
| `reports/axe-before/` | Infracciones de axe antes de las correcciones | 4.4.5 |
| `pact-provider-verification.txt` | Verificación de contratos | 4.4.2 |
| `reports/k6/`, `hpa-*.log` | Carga/estrés y réplicas del HPA (antes y después del ajuste) | 4.4.3 |
| `reports/zap/`, `owasp-top10-prod.txt` | DAST con ZAP y verificaciones OWASP Top 10 | 4.4.4 |
| `reports/lighthouse/` | Auditoría Lighthouse de la pantalla de acceso | 4.4.5 |
