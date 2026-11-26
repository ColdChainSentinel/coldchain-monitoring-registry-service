# ColdChain Sentinel en Kubernetes

Manifiestos Kustomize para Identity & Access, Registry, Telemetry, Alerts y el frontend.
Las APIs exigen una sesión Bearer emitida por Identity & Access (EP-01); cada servicio
la valida contra `/api/v1/auth/introspect`.

```mermaid
flowchart LR
    U[Cliente web] --> G[Ingress TLS interno, nube]
    G --> F[Frontend Nginx]
    F --> I[Identity & Access]
    F --> R[Monitoring Registry]
    F --> T[Telemetry Ingestion]
    F --> A[Alert Management]
    T --> R
    T --> A
    A --> R
    R -. introspección .-> I
    T -. introspección .-> I
    A -. introspección .-> I
    I --> D[(PostgreSQL: coldchain_identity)]
    R --> P[(PostgreSQL: coldchain)]
    T --> Q[(PostgreSQL: coldchain_telemetry)]
    A --> S[(PostgreSQL: coldchain_alerts)]
    C[ConfigMap: reglas térmicas] --> A
```

## Capas

| Ruta | Contenido |
|---|---|
| `base/` | Cinco Deployments y Services internos, estrategia RollingUpdate, sondas, solicitudes y límites. HPA por CPU para Telemetry y Alerts, de 1 a 3 réplicas, objetivo 70 %. |
| `overlays/local/` | Namespace `coldchain-local`, imágenes de Docker Desktop, PostgreSQL 16 con PVC de 1 GiB y ConfigMap de reglas de demostración. El cliente se prueba con port-forward. |
| `overlays/cloud/` | Plantilla de namespace `coldchain`, referencias a imágenes GHCR y un Ingress TLS. Requiere clase de Ingress interna, host, certificado, base de datos y Secrets del ambiente. |

Registry usa TCP para vitalidad y HTTP para disponibilidad con base de datos. Identity,
Telemetry y Alerts usan los grupos Actuator de vitalidad y disponibilidad. Los servicios se
comunican con DNS interno del namespace. Los pods de aplicación corren sin root ni
token de cuenta de servicio. La base local usa un usuario de demostración con cuatro
bases lógicas. En la nube, preparar usuarios independientes de mínimo privilegio.

Los HPA necesitan un proveedor de `metrics.k8s.io`; aplicar sus objetos no demuestra
escalado. El Ingress requiere un controlador existente y una clase configurada.

## Prueba en Docker Desktop

Ejecutar en PowerShell desde la raíz del repositorio:

```powershell
kubectl --context docker-desktop get nodes
docker compose -p coldchain-k8s build identity-access monitoring-registry telemetry-ingestion alert-management frontend
pwsh -NoProfile -File infrastructure/kubernetes/Deploy-Local.ps1
pwsh -NoProfile -File infrastructure/kubernetes/Test-Local.ps1
kubectl --context docker-desktop -n coldchain-local get deploy,pods,svc,pvc,hpa
```

El despliegue genera una contraseña local al crear el PostgreSQL de prueba y otra para el
administrador inicial (Secret `identity-access-bootstrap`, usuario `admin`). Las siguientes
ejecuciones conservan ambas y reutilizan el volumen; si el PVC es anterior a Identity, el
script crea la base `coldchain_identity`. La prueba inicia sesión con ese administrador,
registra un punto nuevo, configura su regla de 2 a 8 °C por la API de Alerts (HU-09),
envía 10 °C y comprueba la persistencia y la alerta abierta. Para leer la contraseña del
administrador:

```powershell
kubectl --context docker-desktop -n coldchain-local get secret identity-access-bootstrap -o jsonpath='{.data.BOOTSTRAP_ADMIN_PASSWORD}'
```

El valor está en Base64. El navegador puede
usar un port-forward equivalente con `kubectl --context docker-desktop -n coldchain-local
port-forward service/frontend 18080:8080` y abrir `http://localhost:18080`.

## Entrega por GitHub Actions y preparación en Azure

[`kubernetes.yml`](../../.github/workflows/kubernetes.yml) renderiza las dos capas
y adjunta los YAML a la ejecución. El job de publicación se habilita desde `main` y usa
el SHA completo para construir y publicar las cinco imágenes en GHCR. Si los paquetes
ya existen, verificar que concedan escritura al repositorio y acceso de lectura al
clúster. El YAML cloud que publica el workflow es una plantilla con valores que el
operador debe completar.

Para el primer despliegue de integración en nube:

1. Seleccionar la ejecución con las cinco imágenes publicadas y usar ese mismo SHA
   en las cinco entradas de `overlays/cloud/kustomization.yaml`.
2. Configurar un Ingress **interno** y actualizar clase y hostname en
   `overlays/cloud/ingress.yaml`. Aprovisionar el certificado TLS y el Secret
   `coldchain-tls` en el namespace `coldchain`. Mantener acceso privado hasta
   verificar la autenticación y la exposición de endpoints operativos.
3. Crear el namespace primero. En el sistema de secretos del ambiente, preparar
   `identity-access-db`, `monitoring-registry-db`, `telemetry-ingestion-db` y
   `alert-management-db`. Cada uno debe contener `DB_URL`, `DB_USER` y `DB_PASSWORD`
   propios. Preparar también `identity-access-bootstrap` con
   `BOOTSTRAP_ADMIN_USERNAME` y `BOOTSTRAP_ADMIN_PASSWORD`. Preparar
   además `coldchain-thermal-rules` con la clave `thermal-rules.yaml`; para su
   estructura, consultar la capa local. Establecer los límites de cada punto con
   el responsable funcional. Si los paquetes GHCR son privados, configurar su lectura
   antes de iniciar los Deployments.
4. Confirmar contexto y namespace, renderizar la capa y ejecutar
   `kubectl --context <contexto-azure> apply --dry-run=server -k infrastructure/kubernetes/overlays/cloud`.
   Si valida, aplicar la misma capa sin `--dry-run=server` y comprobar
   `kubectl rollout status` en los cinco Deployments, acceso al Ingress y el flujo real.
5. Para promoción, usar los mismos SHA y manifiestos verificados en el ambiente
   previo. Para rollback, conservar el SHA y la configuración del último despliegue
   sano y reaplicarlos. `kubectl rollout undo` permite revertir la plantilla de un
   Deployment, pero la configuración externa y las migraciones requieren una
   comprobación separada.

La integración con la identidad de Azure, la automatización del paso de despliegue,
la promoción entre ambientes y el ensayo de rollback en nube deben completarse en el
entorno del equipo. La Azure Function conserva su unidad de despliegue independiente.

## Entrega final: ambientes GitOps, observabilidad y nube

| Ruta | Uso |
|---|---|
| `overlays/dev`, `overlays/prod` | Ambientes gestionados por Argo CD (`infrastructure/argocd`). La etiqueta de imagen es el SHA del commit; `scripts/cd/*.sh` la actualizan (build, promote, rollback) y `.github/workflows/cd-gitops.yml` los ejecuta. |
| `overlays/cloud` | AKS (`infrastructure/terraform-aks`): App Routing con TLS, secretos desde Key Vault (CSI) y PostgreSQL sobre `managed-csi`. |
| `components/postgres` | PostgreSQL en el clúster con su NetworkPolicy. |
| `observability/` | Prometheus (SLO y alertas), Alertmanager, Grafana, Loki, Promtail, Jaeger y kube-state-metrics. |

Clúster local de la entrega (k3s en Docker, con Traefik y metrics-server):

```powershell
docker run -d --name coldchain-k3s --privileged -p 6443:6443 -p 8088:80 -v coldchain-k3s-data:/var/lib/rancher/k3s -e K3S_KUBECONFIG_OUTPUT=/output/kubeconfig.yaml -e K3S_KUBECONFIG_MODE=666 -v ${PWD}/.k3s:/output rancher/k3s:v1.31.4-k3s1 server
$env:KUBECONFIG = "$PWD/.k3s/kubeconfig.yaml"
./infrastructure/kubernetes/Deploy-Observability.ps1 -Context default
./infrastructure/kubernetes/Deploy-Local.ps1 -Context default -Overlay dev -SecretsOnly
./infrastructure/kubernetes/Deploy-Local.ps1 -Context default -Overlay prod -SecretsOnly
./infrastructure/argocd/Install-ArgoCD.ps1 -Context default
sh scripts/cd/build-local-images.sh    # imágenes del commit actual dentro de k3s
```

URLs: `http://coldchain.localhost:8088` (prod), `http://dev.coldchain.localhost:8088`,
`grafana`, `prometheus`, `alertmanager`, `jaeger` y `argocd` `.localhost:8088`.
Runbook: `docs/operations/runbook.md`. SLO: `docs/operations/slo.md`.

### AKS

```powershell
cd infrastructure/terraform-aks
terraform init -backend-config=backend.hcl
terraform apply
az aks get-credentials -g rg-coldchain-aks -n (terraform output -raw cluster_name)
```

Reemplazar `KEY_VAULT_NAME`, `TENANT_ID` y `CSI_IDENTITY_CLIENT_ID` en
`overlays/cloud/secret-provider-class.yaml` con `terraform output`, el host del Ingress y
`IMAGE_TAG`; luego registrar una Application de Argo CD apuntando a `overlays/cloud`.
Detener el clúster fuera de uso: `az aks stop`.
