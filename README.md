# ColdChain Sentinel

MVP académico — Agile Software Factories. Monorepo con componentes que se compilan,
prueban y despliegan independientemente. Incluye un emisor serverless de pruebas;
**el negocio de mediciones, alertas, dashboard e infraestructura cloud sigue pendiente**.

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
  terraform/            # Reservado para el equipo
  kubernetes/           # Reservado para el equipo
tests/
  acceptance/           # Reservado
  integration/          # Reservado
docs/
  architecture/         # Guía de integración y verificación
  api-contracts/        # OpenAPI iniciales versionados
  evidence/             # Evidencias históricas originales
.github/workflows/      # CI original de Registry, solo rutas adaptadas
docker-compose.yml      # Marcador, no ejecutable
pom.xml                 # Agregador opcional de los tres proyectos independientes
```

No se renombra la carpeta local ni el remoto Git. No se reescribe historial. Los
archivos Docker y scripts anteriores se conservan en `services/monitoring-registry/`.
El `.env` local previo permanece ignorado en la raíz, sin leerse ni copiarse.

## Responsabilidades y puertos

| Componente | Responsabilidad | Puerto local | Estado |
|---|---|---:|---|
| Monitoring Registry | Registrar/listar/consultar puntos UUID | 8080 | Implementado |
| Telemetry Ingestion | Recibir, validar, almacenar y consultar mediciones | 8081 | Base + contrato |
| Alert Management | Evaluar reglas por punto y registrar alertas | 8082 | Base + contrato |
| Frontend | Interfaz web, ahora solo diagnóstico de salud | 5173 | Base React |

Cada servicio es dueño de su base. Sin tablas, entidades ni librerías de negocio
compartidas. Flujo previsto: frontend → APIs; emisor → Telemetry → Registry (validar
punto) → Alerts (evaluación HTTP); Alerts → Registry (validar punto). Sin broker,
gateway, discovery ni caché. No hay llamadas entre servicios implementadas todavía.

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
Cada nuevo servicio tiene tres smoke tests HTTP con puerto aleatorio: salud/probes,
Swagger/OpenAPI y comprobación de que las rutas de negocio aún no están implementadas.
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

Abrir http://localhost:5173. El botón comprueba los tres `/actuator/health`; si no
están levantados muestra el fallo, no datos simulados.

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
automáticamente. Las nuevas bases aún no tienen migraciones de dominio.

PowerShell, ejemplo para Telemetry con una base creada localmente:

```powershell
cd services/telemetry-ingestion
$env:DB_URL='jdbc:postgresql://localhost:5432/coldchain_telemetry'
$env:DB_USER='USUARIO_LOCAL'
$env:DB_PASSWORD=Read-Host 'Contraseña local'
.\mvnw.cmd spring-boot:run
```

URLs de clientes futuros: `REGISTRY_BASE_URL` default `http://localhost:8080` en los
dos nuevos servicios; `ALERTS_BASE_URL` default `http://localhost:8082` en Telemetry.
No se consumen todavía. No existe configuración de umbrales universal.

Demo de los nuevos esqueletos sin PostgreSQL, desde su carpeta:

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

## Contratos y salud

- [Telemetry OpenAPI 0.1.0](docs/api-contracts/telemetry-ingestion-v1.yaml):
  `POST /api/measurements`, `GET /api/measurements?monitoringPointId={uuid}`.
- [Alerts OpenAPI 0.1.0](docs/api-contracts/alert-management-v1.yaml):
  `POST /api/alerts/evaluate`, `GET /api/alerts?monitoringPointId={uuid}`.
- Registry implementado: `POST/GET /api/v1/monitoring-points`,
  `GET /api/v1/monitoring-points/{uuid}`. API generada: http://localhost:8080/v3/api-docs.
- Todos: `/actuator/health`, `/swagger-ui.html`, `/v3/api-docs`.
- Nuevos servicios además: `/actuator/health/liveness` y `/actuator/health/readiness`.

Los YAML de negocio son contratos de trabajo, no rutas implementadas. Identificadores
UUID, temperaturas decimales Celsius, fechas RFC 3339/UTC. Alertas exigen regla
explícita por punto, fuera del modelo actual de Registry. Consultar semántica de
errores, idempotencia y evaluación pendiente en la guía, antes de desarrollar.

## Integración del equipo y pendientes

La [guía de integración](docs/architecture/integration-guide.md) define configuración,
límites de responsabilidad, manejo de fallos, probes y entregables para Docker,
CI/CD, Azure/IaC y testing. Registry conserva su workflow y la contribución
serverless añade un CI independiente. No se crean imágenes ni recursos cloud nuevos.

Pendientes: negocio Telemetry/Alerts, reglas asignadas por el responsable funcional,
pruebas reales PostgreSQL y entre servicios, dashboard, seguridad y despliegues.
No se debe exponer el MVP públicamente sin controles de acceso acordados.

Resultados de esta entrega: [verificación](docs/architecture/verification.md).

## Contribución serverless y Software Factory (3.5 y 3.8)

La [función emisora](functions/telemetry-simulator/README.md) permite construir mediciones
en dry-run o enviarlas al contrato de Telemetry mediante un temporizador. Incluye diez
pruebas y un workflow propio de CI. No contiene reglas térmicas ni notificaciones.
La integración real requiere que el equipo implemente Telemetry/Alerts.

La [redacción de los apartados 3.5 y 3.8](docs/report/3.5-3.8-joaquin-carbajal.md)
documenta arquitectura, pruebas, actualización del proceso y pendientes de despliegue.
