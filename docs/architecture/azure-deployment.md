# Despliegue en Azure

Registro del despliegue del MVP de ColdChain Sentinel en la suscripción **Azure for
Students** (rama `feat/azure-deployment`, 30 de octubre de 2026). Los pasos para repetirlo
y la operación diaria están en el [README de Terraform](../../infrastructure/terraform/README.md);
este documento resume qué se hizo, por qué y qué se verificó. El
[redespliegue del 27 de noviembre de 2026](#redespliegue-27-de-noviembre-de-2026) actualizó
el entorno a `develop` e incorporó `identity-access`.

## Resultado

Toda la aplicación corre en el grupo `rg-coldchain-dev` (región `brazilsouth`),
definida como Terraform en `infrastructure/terraform/`:

| Componente | Servicio de Azure | Acceso |
|---|---|---|
| Frontend (nginx + SPA, proxy a las cuatro APIs) | Container App | Público HTTPS |
| `identity-access` | Container App | Público HTTPS (desde el redespliegue) |
| `monitoring-registry` | Container App | Público HTTPS (interno hasta el redespliegue) |
| `telemetry-ingestion` | Container App | Público HTTPS (lo usa el simulador) |
| `alert-management` | Container App | Público HTTPS (interno hasta el redespliegue) |
| Bases `coldchain`, `coldchain_telemetry`, `coldchain_alerts`, `coldchain_identity` | PostgreSQL Flexible Server B1MS, v16 | TLS obligatorio |
| Simulador de telemetría | Azure Function (Flex Consumption, Python) | Saliente hacia Telemetry |
| Logs, presupuesto | Log Analytics, presupuesto USD 5 | — |

Las URL públicas se obtienen con `terraform output frontend_url` y
`terraform output telemetry_url`. Desde el redespliegue, las APIs exigen una sesión de
Identity en `/api/**` (ver [riesgos](#riesgos-abiertos)).

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
  migrar a un backend remoto ([tarea pendiente](terraform-remote-state.md)).

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

## Redespliegue (27 de noviembre de 2026)

Actualización del entorno a `origin/develop` en `f704e82` (PR #13), desde la rama
`chore/azure-redeploy-develop`. Incluye EP-01 (servicio `identity-access`: sesiones, roles y
permisos por punto), HU-06 (dispositivos) y HU-09 (rangos térmicos configurables).

- **Imágenes:** `ghcr.io/coldchainsentinel/<app>:f704e82737b5` (`linux/amd64`) para las cinco
  apps. El paquete nuevo `identity-access` se hizo público; las cinco admiten descarga
  anónima.
- **Recursos nuevos:** Container App `identity-access` (puerto 8083), base
  `coldchain_identity` con el rol `identity_app` (mismo patrón: dueño de su base y de su
  esquema `public`, sin `CONNECT` para `PUBLIC`), contraseña de base y contraseña del
  administrador inicial generadas por Terraform y guardadas como secretos de Container Apps.
- **Cambios en recursos existentes:** imágenes nuevas; `IDENTITY_BASE_URL` y
  `COLDCHAIN_AUTH_READTIMEOUT` en Registry, Telemetry y Alerts; `IDENTITY_UPSTREAM` en el
  frontend; pool Hikari de 4 conexiones (cuatro servicios en B1MS); sonda de liveness del
  frontend en el puerto 8080; ingreso externo en Registry, Alerts e Identity; ajustes del
  simulador para iniciar sesión (sigue deshabilitado); regla de firewall del operador
  actualizada a su IP actual.
- **Migraciones aplicadas por Flyway al arrancar:** Identity V1, Registry V2 (dispositivos y
  estado del punto) y Alerts V2 (reglas térmicas en base de datos).
- **Estado de Terraform:** se siguió usando el estado local (migración al backend remoto
  pendiente) mediante un `backend_override.tf` ignorado por Git. Se respaldó antes y después
  del cambio fuera del repositorio.

### Incidentes

- **Ingreso interno inaccesible en Express:** tras el primer `apply`, el proxy del frontend
  y las llamadas entre servicios (incluida la validación de sesiones) recibían
  `404 {"error":"Not found"}` del borde de Azure, con las apps internas en ejecución. Todos
  los nombres `<app>.<dominio>` resuelven al mismo borde público y este ya no enruta hacia
  apps internas. Se activó el ingreso externo de Registry, Alerts e Identity: las rutas ya
  eran públicas mediante el proxy y `/api/**` exige sesión.
- **Sonda del frontend en el puerto 80:** `develop` cambió la imagen a nginx sin privilegios
  (puerto 8080) y el destino del ingreso, pero no la sonda de liveness; se corrigió antes de
  aplicar para evitar reinicios.

### Verificación

| Verificación | Resultado |
|---|---|
| Pruebas | `mvnw -Pacceptance verify`: 225 pruebas (32 Registry, 93 Telemetry, 94 Alerts, 6 Identity); integración con PostgreSQL: 28; frontend: 65 pruebas y build correctos |
| Plan inicial | 6 por crear, 5 por cambiar en sitio, 2 por destruir (regla de firewall antigua y `terraform_data.db_roles`, que se vuelve a ejecutar); sin reemplazos de recursos con estado |
| Plan posterior | Solo el diff cosmético `transport = "HTTP"` de las sondas |
| Revisiones | Las cinco apps con la imagen `f704e82737b5`, revisión `Healthy` y `Running` |
| Frontend y salud por el proxy | `/` 200; `/identity`, `/registry`, `/telemetry` y `/alerts` `/actuator/health` en `UP` (~60 s en frío) |
| Inicio de sesión | `admin` (ADMIN) 200; contraseña incorrecta 401 |
| Sin token o token inválido | 401 en Registry, Alerts y Telemetry |
| Con token | Puntos (CAM-DEMO-01 intacto), dispositivos HU-06, regla HU-09 (2-8 °C), alertas y mediciones responden 200; validaciones devuelven 400 |
| Flujo completo | Medición de 5 °C en CAM-DEMO-01: 201 con evaluación `COMPLETED` |
| Cierre de sesión | 204; el token revocado recibe 401 |
| Costos | `min_replicas = 0` y simulador deshabilitado |

## Riesgos abiertos

- Las cuatro APIs tienen ingreso público propio y exponen Swagger y Actuator (`health`,
  `info`) sin sesión; `/api/**` exige sesión. El login no limita intentos. Un entorno con VNet
  y sin modalidad Express permitiría volver al ingreso interno.
- PostgreSQL acepta conexiones desde servicios de Azure de cualquier tenant; la protección
  son contraseñas de 32 caracteres y TLS. Una VNet con acceso privado lo eliminaría.
- JDBC usa `sslmode=require`: cifra, pero no valida el certificado del servidor.
- Estado de Terraform local con contraseñas: migrarlo a un backend remoto es una tarea
  pendiente, documentada paso a paso en [estado remoto de Terraform](terraform-remote-state.md).
- El timeout de 30 s del simulador es menor que un arranque en frío: su primera ejecución
  tras un periodo inactivo puede fallar.
