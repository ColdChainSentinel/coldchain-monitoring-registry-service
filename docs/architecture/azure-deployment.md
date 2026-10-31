# Despliegue en Azure

Registro del despliegue del MVP de ColdChain Sentinel en la suscripción **Azure for
Students** (rama `feat/azure-deployment`, 30 de octubre de 2026). Los pasos para repetirlo
y la operación diaria están en el [README de Terraform](../../infrastructure/terraform/README.md);
este documento resume qué se hizo, por qué y qué se verificó.

## Resultado

Toda la aplicación corre en el grupo `rg-coldchain-dev` (región `brazilsouth`),
definida como Terraform en `infrastructure/terraform/`:

| Componente | Servicio de Azure | Acceso |
|---|---|---|
| Frontend (nginx + SPA, proxy a las tres APIs) | Container App | Público HTTPS |
| `monitoring-registry` | Container App | Interno al entorno |
| `telemetry-ingestion` | Container App | Público HTTPS (lo usa el simulador) |
| `alert-management` | Container App | Interno al entorno |
| Bases `coldchain`, `coldchain_telemetry`, `coldchain_alerts` | PostgreSQL Flexible Server B1MS, v16 | TLS obligatorio |
| Simulador de telemetría | Azure Function (Flex Consumption, Python) | Saliente hacia Telemetry |
| Logs, presupuesto | Log Analytics, presupuesto USD 5 | — |

Las URL públicas se obtienen con `terraform output frontend_url` y
`terraform output telemetry_url`. No se publican aquí porque el sistema aún no tiene
autenticación (ver [riesgos](#riesgos-abiertos)).

## Decisiones

- **Costo cercano a cero:** Container Apps en plan Consumption con `min_replicas = 0`,
  PostgreSQL B1MS y Function Flex Consumption. El crédito disponible es de USD 100.
- **Región `brazilsouth`:** la política de la suscripción solo permite `brazilsouth`,
  `northcentralus`, `chilecentral`, `mexicocentral` y `westus`.
- **Imágenes en GHCR** (`ghcr.io/coldchainsentinel/*`), públicas y construidas para
  `linux/amd64`. Los Dockerfiles compilan en la plataforma nativa (`$BUILDPLATFORM`) y solo
  la imagen final es amd64, lo que acelera el build en equipos ARM.
- **Aislamiento de datos:** un rol de PostgreSQL por servicio (`registry_app`,
  `telemetry_app`, `alerts_app`), dueño únicamente de su base y sin `CONNECT` para
  `PUBLIC`, según la guía de integración. Flyway crea las tablas al arrancar.
- **Secretos:** las contraseñas se generan en Terraform y viven como secretos de Container
  Apps; nunca en Git ni en variables `VITE_*`. El estado local de Terraform, los planes y
  `terraform.tfvars` están en `.gitignore`.
- **Reglas térmicas sin reconstruir imágenes:** se declaran por UUID del punto en
  `terraform.tfvars` y llegan a Alerts como `SPRING_APPLICATION_JSON`.
- **Telemetry con ingreso público:** el simulador exige una URL base sin ruta, por lo que no
  puede usar el proxy `/telemetry` del frontend.

## Cambios en el código

| Commit | Cambio |
|---|---|
| `dbc2df8` | `.gitignore`: seguimiento local y estado de Terraform fuera de Git |
| `09363e1` | Dockerfiles: etapas de build en la plataforma nativa |
| `10b79f7` | Terraform: Container Apps, PostgreSQL, Function y presupuesto |
| `d02d3b1` | Liveness de Registry independiente de la base; pool Hikari de 5 conexiones |
| `e64c754` | Destino de logs de Container Apps exigido por azurerm v5 |
| `56378f4` | Adaptación al entorno Express y al esquema `public` administrado por Azure |
| `f792923` | Documentación de despliegue, operación y riesgos |
| `a042735` | Timeout del proxy nginx ampliado a 120 s por arranques en frío |
| `b1aaab5` | Sondas HTTP de Container Apps restauradas |

## Incidentes resueltos

- **Entorno Express:** Azure creó el entorno de Container Apps en modalidad Express, que
  rechaza HTTP plano (`allowInsecure`) y publica las apps en `<app>.<dominio-del-entorno>`
  sin `.internal.`. Las llamadas entre servicios pasaron a HTTPS con ese dominio.
- **Esquema `public`:** en PostgreSQL Flexible el esquema `public` pertenece a
  `azure_pg_admin`, lo que impedía a Flyway crear tablas. El script de arranque transfiere
  su propiedad al rol de cada servicio.
- **504 en arranque en frío:** con todas las apps en cero réplicas, una solicitud
  encadenada superaba los 60 s del proxy nginx. El timeout de lectura subió a 120 s.
- **Sondas convertidas a TCP:** ignorar el campo `transport` de las sondas en Terraform
  hacía que Azure las tratara como TCP. Se revirtió.
- **Pérdida del estado local de Terraform:** el archivo `terraform.tfstate` desapareció
  durante una sesión. Se recuperó desde el estado incluido en un plan guardado y se
  conciliaron los recursos sin crear ni destruir nada en Azure. Es la razón principal para
  migrar a un backend remoto.

## Verificación en Azure

| Verificación | Resultado |
|---|---|
| Frontend | HTTP 200 |
| Salud de las tres APIs a través del proxy | `UP` |
| Imágenes en GHCR | Descarga anónima correcta (4 apps) |
| Aislamiento de bases | Cada rol es dueño de su base; acceso cruzado responde `permission denied for database` |
| Evaluación sin regla térmica | 422 `THERMAL_RULE_NOT_CONFIGURED` |
| Evaluación con regla configurada | Alertas `HIGH` y `LOW` en estado `OPEN` |
| Simulador | Host y código desplegados (`emit_measurement` registrada); ejecución en vivo pendiente |

El cambio de infraestructura con credenciales, firewall e ingreso público (`10b79f7`) pasó
una revisión de riesgo y resiliencia: aprobada, sin bloqueos y con 8 observaciones no
bloqueantes; de ellas se aplicaron la liveness por TCP de Registry y el límite del pool.

## Riesgos abiertos

- Sin autenticación: el frontend expone las tres APIs, incluidos Swagger y Actuator.
- PostgreSQL acepta conexiones desde servicios de Azure de cualquier tenant; la protección
  son contraseñas de 32 caracteres y TLS. Una VNet con acceso privado lo eliminaría.
- JDBC usa `sslmode=require`: cifra, pero no valida el certificado del servidor.
- Estado de Terraform local con contraseñas; conviene un backend remoto para el equipo.
- El timeout de 30 s del simulador es menor que un arranque en frío: su primera ejecución
  tras un periodo inactivo puede fallar.
