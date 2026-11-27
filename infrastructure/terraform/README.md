# Terraform — despliegue en Azure

Entorno `dev` de ColdChain Sentinel en una suscripción **Azure for Students**, diseñado
para mantenerse dentro de los montos gratuitos. Las entradas de cada servicio están
en la [guía de integración](../../docs/architecture/integration-guide.md).

## Qué se despliega

| Recurso | Detalle | Acceso |
|---|---|---|
| `frontend` (Container App) | nginx + SPA; proxy de `/registry`, `/telemetry`, `/alerts`, `/identity` | Público HTTPS |
| `identity-access` (Container App) | Puerto 8083, base `coldchain_identity`; sesiones, roles y permisos por punto | Interno al entorno (expuesto por el proxy `/identity`) |
| `monitoring-registry` (Container App) | Puerto 8080, base `coldchain` | Interno al entorno |
| `telemetry-ingestion` (Container App) | Puerto 8081, base `coldchain_telemetry` | Público HTTPS (lo usa el simulador) |
| `alert-management` (Container App) | Puerto 8082, base `coldchain_alerts`, reglas por UUID | Interno al entorno |
| PostgreSQL Flexible Server | B1MS, PostgreSQL 16, 32 GB, TLS obligatorio | Servicios de Azure + IP del operador |
| Function `telemetry-simulator` | Flex Consumption, Python 3.12, timer | Saliente hacia Telemetry |
| Log Analytics, Container Apps Environment, Storage, presupuesto | Soporte | — |

Todo vive en el grupo `rg-coldchain-dev` (región `brazilsouth`; la política de la
suscripción solo permite `brazilsouth`, `northcentralus`, `chilecentral`, `mexicocentral`
y `westus`). Las apps escalan a cero (`min_replicas = 0`).

Cada servicio usa su propio rol de PostgreSQL (`registry_app`, `telemetry_app`,
`alerts_app`, `identity_app`), dueño únicamente de su base; `PUBLIC` no tiene `CONNECT`. Terraform crea
los roles con `scripts/bootstrap-db-roles.sql` y Flyway crea las tablas al arrancar.

Registry, Telemetry y Alerts exigen un token `Bearer` en `/api/**` y lo validan contra
`GET /api/v1/auth/introspect` de Identity (`IDENTITY_BASE_URL`). El token es una sesión
opaca emitida por `POST /api/v1/auth/login`; no hay JWT ni clave de firma compartida.

## Requisitos

- Terraform >= 1.9, Azure CLI con `az login`, Docker con buildx y `psql` en el `PATH`.
- Permiso `write:packages` en GitHub para publicar imágenes en `ghcr.io/coldchainsentinel`
  (paquetes públicos; si fueran privados, usar `registry_username`/`registry_password`
  con un token de solo `read:packages`).
- Proveedores registrados una vez por suscripción:

```sh
for p in Microsoft.App Microsoft.OperationalInsights Microsoft.DBforPostgreSQL \
  Microsoft.Web Microsoft.Storage Microsoft.Consumption; do az provider register -n "$p"; done
```

## Despliegue

1. Variables: copiar `terraform.tfvars.example` a `terraform.tfvars` (ignorado por Git) y
   completar suscripción, correo del presupuesto, IP pública del operador y `image_tag`.
2. Backend: copiar `backend.hcl.example` a `backend.hcl` (ignorado por Git) y completar el
   nombre de la cuenta de almacenamiento. El estado vive en Azure Blob Storage
   (`rg-coldchain-tfstate`), con autenticación de Entra ID y bloqueo por *lease*; hace falta
   el rol **Storage Blob Data Contributor** sobre la cuenta (ver
   [estado remoto de Terraform](../../docs/architecture/terraform-remote-state.md)).

   Mientras esa migración siga pendiente, el estado vigente es local. Para usarlo sin
   migrarlo, crear un `backend_override.tf` (ignorado por Git) y ejecutar
   `terraform init -reconfigure`:

```hcl
terraform {
  backend "local" {
    path = "terraform.tfstate"
  }
}
```

3. Imágenes `linux/amd64` (la etapa de build corre nativa, así que es rápido en ARM):

```sh
TAG=$(git rev-parse --short=12 HEAD)
for app in identity-access monitoring-registry telemetry-ingestion alert-management; do
  docker buildx build --platform linux/amd64 -t ghcr.io/coldchainsentinel/$app:$TAG --push services/$app
done
docker buildx build --platform linux/amd64 -t ghcr.io/coldchainsentinel/frontend:$TAG --push frontend
```

   Un paquete nuevo de GHCR nace privado: hacerlo público (GitHub → paquete →
   *Package settings* → *Change visibility*) o configurar `registry_username`/`registry_password`.

4. Infraestructura:

```sh
terraform init -backend-config=backend.hcl
terraform apply
terraform output frontend_url
```

   El plan siempre muestra `transport = "HTTP"` en las sondas: el entorno Express omite ese
   campo en su respuesta, y ignorarlo convertiría las sondas en TCP. Es un diff cosmético.

5. Código del simulador (Terraform solo crea el host):

```sh
cd ../../functions/telemetry-simulator
zip -q /tmp/simulator.zip function_app.py simulator.py host.json requirements.txt
az functionapp deployment source config-zip -g rg-coldchain-dev \
  -n "$(terraform -chdir=../../infrastructure/terraform output -raw simulator_function_name)" \
  --src /tmp/simulator.zip --build-remote true
```

## Operación

- **Administrador inicial:** Identity crea el usuario ADMIN `bootstrap_admin_username`
  (por defecto `admin`) en su primer arranque, solo si no existe. La contraseña la genera
  Terraform y se guarda como secreto de Container Apps; se lee con
  `terraform output -raw bootstrap_admin_password`. Cambiarla en Terraform no modifica un
  usuario ya creado.
- **Reglas térmicas:** desde HU-09 se configuran en la aplicación
  (`PUT /alerts/api/alerts/rules/{uuid}`, cualquier rol salvo CONDUCTOR con acceso al punto) y se guardan en la base de
  Alerts. Las declaradas en `terraform.tfvars` quedan como respaldo cuando el punto no tiene
  regla en la base; `terraform apply` las envía como `SPRING_APPLICATION_JSON`. Sin ninguna
  regla, la evaluación devuelve 422 `THERMAL_RULE_NOT_CONFIGURED`.

```hcl
thermal_rules = {
  "<uuid-del-punto>" = { threshold_min = 2.0, threshold_max = 8.0 }
}
```

- **Simulador:** queda deshabilitado por defecto. Para una demo, `simulator_enabled = true`
  y `simulator_point_id = "<uuid>"`. Inicia sesión como el administrador inicial a través de
  `<frontend>/identity`; la contraseña solo se carga en la Function mientras el timer está
  activo. Cada ejecución despierta las APIs y consume el monto
  gratuito: apagarlo al terminar.
- **Arranque en frío:** con cero réplicas la primera solicitud tras un periodo inactivo
  tarda ~30-60 s (Spring Boot + Flyway). Para una demo, `min_replicas = 1` y volver a `0`
  después. Las llamadas entre servicios y la validación del token contra Identity
  (`COLDCHAIN_AUTH_READTIMEOUT`) usan `http_read_timeout = 45s` por este motivo.
- **Entorno Express:** Azure crea el entorno de Container Apps en modalidad Express. No
  admite HTTP plano (las apps se llaman por HTTPS en `<app>.<dominio-del-entorno>`), no
  tiene `az containerapp logs show` ni reinicio de revisiones (consultar los logs en Log
  Analytics, tabla `ContainerAppConsoleLogs_CL`) y un cambio de configuración se aplica
  cuando la réplica vuelve a arrancar, normalmente después de escalar a cero (~5 min sin
  tráfico).
- **Costos:** presupuesto mensual de USD 5 con avisos por correo; Log Analytics limitado a
  0.5 GB diarios. Eliminar todo con `terraform destroy`.

## Riesgos conocidos

- Las APIs exigen sesión en `/api/**`, pero Swagger (`/swagger-ui.html`, `/v3/api-docs`) y
  Actuator (`health`, `info`) siguen públicos a través del proxy del frontend. El login no
  tiene límite de intentos.
- PostgreSQL usa acceso público con la regla "servicios de Azure" (`0.0.0.0`), que admite
  conexiones desde cualquier tenant de Azure; la protección son las contraseñas de 32
  caracteres y TLS. Una VNet con acceso privado eliminaría esa exposición.
- JDBC usa `sslmode=require` (cifra, pero no valida el certificado del servidor).
- El estado de Terraform contiene contraseñas: nunca versionarlo (`*.tfstate` está en
  `.gitignore`). El backend remoto ya está declarado, pero la migración del estado
  existente (pasos 4 y 5 de [estado remoto de Terraform](../../docs/architecture/terraform-remote-state.md))
  sigue pendiente: hasta entonces, nadie debe ejecutar `apply` desde otra máquina.
- El simulador tiene un timeout máximo de 30 s, menor que un arranque en frío; su primera
  ejecución tras inactividad puede fallar sin reintento (comportamiento del contrato).
