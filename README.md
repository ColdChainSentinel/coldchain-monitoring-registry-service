# ColdChain Sentinel

MVP académico — Agile Software Factories. Monorepo con componentes que se compilan,
prueban y despliegan independientemente. Registry, ingesta/consulta de mediciones y
evaluación/consulta de alertas y frontend funcional están implementados. El flujo local
integrado está verificado; **infraestructura y despliegues siguen fuera de esta entrega**.

## Estructura

```text
services/
  monitoring-registry/   # Servicio existente, código y tests conservados
  telemetry-ingestion/   # Esqueleto Spring Boot, sin negocio
  alert-management/     # Esqueleto Spring Boot, sin negocio
frontend/               # React + Vite + TypeScript, página de diagnóstico
functions/
  telemetry-simulator/  # Azure Functions: emisor de mediciones con Timer Trigger
infrastructure/
  terraform/            # Despliegue Azure (Container Apps, PostgreSQL, Function)
  kubernetes/           # Reservado para el equipo
tests/
  acceptance/           # HU-04 automatizada + diseños EP03 pendientes
  integration/          # Aceptación HTTP entre servicios, PostgreSQL/H2
docs/
  architecture/         # Guía de integración y verificación
  api-contracts/        # OpenAPI iniciales versionados
  evidence/             # Evidencias históricas originales
.github/workflows/      # CI de Registry + aceptación HU-04
docker-compose.yml      # Orquestación local ejecutable (ver sección Docker Compose)
config/alerts/          # Plantilla de reglas térmicas para Compose local
pom.xml                 # Agregador opcional de los tres proyectos independientes
```

No se renombra la carpeta local ni el remoto Git. No se reescribe historial. Los
archivos Docker y scripts anteriores se conservan en `services/monitoring-registry/`.
El `.env` local previo permanece ignorado en la raíz, sin leerse ni copiarse.

## Responsabilidades y puertos

| Componente | Responsabilidad | Puerto local | Estado |
|---|---|---:|---|
| Monitoring Registry | Registrar/listar/consultar puntos UUID | 8080 | Implementado |
| Telemetry Ingestion | Recibir, validar, almacenar y consultar mediciones | 8081 | Implementado |
| Alert Management | Evaluar reglas por punto y registrar alertas | 8082 | Implementado |
| Frontend | Dashboard, puntos, mediciones y alertas por punto | 5173 | Implementado |

Cada servicio es dueño de su base. Sin tablas, entidades ni librerías de negocio
compartidas. Flujo previsto: frontend → APIs; emisor → Telemetry → Registry (validar
punto) → Alerts (evaluación HTTP); Alerts → Registry (validar punto). Sin broker,
gateway, discovery ni caché. Telemetry ya valida puntos en Registry y solicita evaluación
a Alerts; mientras este no esté disponible conserva las mediciones con estado PENDING.
Alerts aplica reglas explícitas por UUID y persiste resultados idempotentes, con o sin alerta.

## Requisitos

- JDK **17**, acceso a Maven Central; Maven Wrapper **3.9.11** incluido por proyecto.
- Spring Boot **3.5.16**, springdoc **2.8.17**, iguales al servicio original.
- PostgreSQL **16** para ejecutar normalmente, una base/usuario por servicio.
  Tests usan H2, no necesitan PostgreSQL, Docker ni otros servicios.
- Node **22.12+** y npm para frontend; usar `npm ci` con el lockfile versionado.

## Compilación y pruebas

Desde la raíz (Linux/macOS):

```sh
sh mvnw clean verify
```

Windows PowerShell: `.\mvnw.cmd clean verify`.
O ejecutar independientemente dentro de cualquiera de las tres carpetas de servicio:

```sh
sh mvnw clean verify
sh mvnw spring-boot:run
```

En Windows usar `.\mvnw.cmd` en lugar de `sh mvnw`. No se requiere instalar ni compilar
otro servicio para construir uno. JARs: `services/<servicio>/target/`:
`monitoring-management-0.1.0.jar` (nombre original conservado),
`telemetry-ingestion-0.1.0.jar`, `alert-management-0.1.0.jar`.

Registry mantiene Checkstyle, 20 pruebas y gate JaCoCo de 70% de líneas.
Telemetry añade unitarias, persistencia y pruebas HTTP de endpoints/clientes con
dobles locales de Registry/Alerts. Puede repetirse con PostgreSQL real mediante
TEST_DB_* sobre una base exclusiva y desechable; ver su README. Alerts añade pruebas
de reglas, persistencia, endpoints e idempotencia secuencial/concurrente. Para PostgreSQL,
ejecutar cada servicio por separado con su propia TEST_DB_URL, nunca ambos sobre la misma base.
Los reportes se generan en `services/<servicio>/target/surefire-reports/`; cobertura
actual solo en Registry: `target/site/jacoco/index.html` dentro de su carpeta.

Frontend, desde `frontend/`:

```sh
npm ci
npm run typecheck
npm test
npm run build
npm run dev
```

Abrir http://localhost:5173. Permite registrar puntos y mediciones y consultar alertas;
comprueba salud y muestra errores explícitos si las APIs no están disponibles, sin
datos simulados. Detalles y capturas: [frontend](frontend/README.md).

## Variables y arranque local

Definir por proceso `DB_URL`, `DB_USER`, `DB_PASSWORD`; opcional `PORT`.
Bases locales previstas:

| Servicio | `DB_URL` predeterminada | Usuario |
|---|---|---|
| Registry | `jdbc:postgresql://localhost:5432/coldchain` | `DB_USER`, default original coldchain |
| Telemetry | `jdbc:postgresql://localhost:5432/coldchain_telemetry` | `DB_USER` requerida |
| Alerts | `jdbc:postgresql://localhost:5432/coldchain_alerts` | `DB_USER` requerida |

`DB_PASSWORD` es requerida, sin contraseña predeterminada. Crear previamente bases y
usuarios; no compartir credenciales de producción en Git. Maven/Java no leen `.env`
automáticamente. Telemetry incluye migración Flyway de mediciones; Alerts incluye
alertas y evaluaciones, en bases independientes.

PowerShell, ejemplo para Telemetry con una base creada localmente:

```powershell
cd services/telemetry-ingestion
$env:DB_URL='jdbc:postgresql://localhost:5432/coldchain_telemetry'
$env:DB_USER='USUARIO_LOCAL'
$env:DB_PASSWORD=Read-Host 'Contraseña local'
.\mvnw.cmd spring-boot:run
```

URLs de clientes: `REGISTRY_BASE_URL` default `http://localhost:8080` en los
dos nuevos servicios; `ALERTS_BASE_URL` default `http://localhost:8082` en Telemetry.
Telemetry ya las consume durante POST. Timeouts opcionales: `HTTP_CONNECT_TIMEOUT=2s`
y `HTTP_READ_TIMEOUT=5s`. Alerts también consume Registry con esos timeouts. Sin umbrales universales.
Configurar sus reglas por UUID en un archivo externo mediante `SPRING_CONFIG_ADDITIONAL_LOCATION`;
Registry no tiene campos térmicos. Ver el [README de Alerts](services/alert-management/README.md).

Demo de Telemetry/Alerts sin PostgreSQL, desde su carpeta:

```sh
sh mvnw spring-boot:test-run -Dspring-boot.run.profiles=smoke
```

Este comando utiliza H2 del classpath de test; no es un modo de producción.
Para Registry, la demo equivalente usa `-Dspring-boot.run.profiles=test`.
Las pruebas automáticas ya verifican salud sin arrancar procesos manuales.

Frontend: `.env.example` enumera `VITE_REGISTRY_BASE_URL=/registry`,
`VITE_TELEMETRY_BASE_URL=/telemetry`, `VITE_ALERTS_BASE_URL=/alerts`.
El proxy Vite los dirige a 8080/8081/8082 evitando cambios en CORS de Registry.
En producción se debe configurar proxy equivalente o URLs y CORS explícitos;
las variables `VITE_*` son públicas y de compilación, nunca secretos.

## Docker Compose (entorno local integrado)

Requiere Docker con Compose v2. Desde la raíz:

```sh
cp .env.example .env            # opcional: personalizar puertos/credenciales locales
docker compose up -d --build    # postgres + 3 servicios + frontend en http://localhost
```

1. Registrar un punto en el frontend y copiar su UUID.
2. `cp config/alerts/thermal-rules.example.yml config/alerts/thermal-rules.yml`,
   reemplazar el UUID y ajustar límites aprobados (archivo ignorado por Git).
3. `docker compose restart alert-management` para cargar la regla.
4. Opcional, simulador: definir `SIMULATOR_POINT_ID` y `SIMULATOR_DRY_RUN=false` en `.env`
   y ejecutar `docker compose --profile simulator up -d`.

El script de bases solo corre con el volumen vacío. Si ya existía un volumen anterior,
recrearlo con `docker compose down -v` (borra los datos locales). Las credenciales por
defecto de Compose son solo de desarrollo local.

## Contratos y salud

- [Telemetry OpenAPI 0.1.1](docs/api-contracts/telemetry-ingestion-v1.yaml):
  `POST /api/measurements`, `GET /api/measurements?monitoringPointId={uuid}`.
- [Alerts OpenAPI 0.2.0](docs/api-contracts/alert-management-v1.yaml):
  `POST /api/alerts/evaluate`, `GET /api/alerts?monitoringPointId={uuid}`.
- Registry implementado: `POST/GET /api/v1/monitoring-points`,
  `GET /api/v1/monitoring-points/{uuid}`. API generada: http://localhost:8080/v3/api-docs.
- Todos: `/actuator/health`, `/swagger-ui.html`, `/v3/api-docs`.
- Nuevos servicios además: `/actuator/health/liveness` y `/actuator/health/readiness`.

Ambos YAML corresponden a rutas implementadas; Alerts añade el campo opcional `type`
para clasificar LOW_TEMPERATURE / HIGH_TEMPERATURE sin cambiar el cliente de Telemetry.
Identificadores
UUID, temperaturas decimales Celsius, fechas RFC 3339/UTC. Alertas exigen regla
explícita por punto, fuera del modelo actual de Registry. Consultar semántica de
errores, idempotencia y evaluación pendiente en la guía, antes de desarrollar.

## Integración del equipo y pendientes

La [guía de integración](docs/architecture/integration-guide.md) define configuración,
límites de responsabilidad, manejo de fallos, probes y entregables para Docker,
CI/CD, Azure/IaC y testing. Registry conserva su workflow y la contribución
serverless añade un CI independiente. No se crean imágenes ni recursos cloud nuevos.

Pendientes: reglas asignadas por el responsable funcional,
aceptación formal del entorno desplegado, seguridad y despliegues. Instrucciones y decisiones
de Telemetry: [README del servicio](services/telemetry-ingestion/README.md) y
[documentación](docs/architecture/telemetry-ingestion.md).
No se debe exponer el MVP públicamente sin controles de acceso acordados.

Resultados históricos de fase 1: [verificación](docs/architecture/verification.md).
Implementación y verificación de Telemetry: [documentación](docs/architecture/telemetry-ingestion.md).
Implementación y verificación de Alerts: [documentación](docs/architecture/alert-management.md).
Flujo completo React → servicios → PostgreSQL, reinicios y resultados actuales:
[integración funcional del MVP](docs/architecture/mvp-integration.md).

## Aceptación automatizada (HU-04)

Desde la raíz: `.\mvnw.cmd -B -ntp -pl services/monitoring-registry -Pacceptance clean verify`.
Ejecuta los tests actuales de Registry y tres escenarios Cucumber con tablas de datos.
Reportes y pendientes EP03: [tests/acceptance](tests/acceptance/README.md).
El workflow independiente `ColdChain Acceptance` conserva el CI original y publica los
reportes. Su ejecución remota se verificará después de subir la rama.

Aceptación integrada de mediciones/alertas (12 escenarios, alcance parcial de HU-12
y HU-19): [ejecución PostgreSQL y H2](tests/integration/README.md). El modo PostgreSQL
requiere Docker activo; H2 es una comprobación explícita distinta, no un reemplazo
silencioso de PostgreSQL. El workflow incluye un job PostgreSQL pendiente de validación remota.

Verificación local de aceptación integrada: 12/12 escenarios aprobados con PostgreSQL/Testcontainers, además de la ejecución previa H2. [Evidencia](docs/evidence/acceptance/integrated-local.md). El resultado remoto en Actions sigue pendiente.
Resultados de esta entrega: [verificación](docs/architecture/verification.md).
Despliegue en Azure: [registro y verificación](docs/architecture/azure-deployment.md).

## Contribución serverless y Software Factory (3.5 y 3.8)

La [función emisora](functions/telemetry-simulator/README.md) permite construir mediciones
en dry-run o enviarlas al contrato de Telemetry mediante un temporizador. Incluye diez
pruebas y un workflow propio de CI. No contiene reglas térmicas ni notificaciones.
La integración real requiere que el equipo implemente Telemetry/Alerts.

La [redacción de los apartados 3.5 y 3.8](docs/report/3.5-3.8-joaquin-carbajal.md)
documenta arquitectura, pruebas, actualización del proceso y pendientes de despliegue.
