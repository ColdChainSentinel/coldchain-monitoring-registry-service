# Guía de integración — fase 1

## Estado y decisiones

Este repositorio es el monorepo ColdChain Sentinel. Se conserva su nombre físico para
no romper rutas de trabajo ni cambiar el remoto. Registry se mueve dentro del mismo
Git: no se importa otro repositorio ni se reescribe el historial. Para consultar su
historia: `git log --follow -- services/monitoring-registry/pom.xml`.

Registry mantiene Java 17, Spring Boot 3.5.16, Maven Wrapper 3.9.11, springdoc 2.8.17,
PostgreSQL 16, Flyway, UUID y paquetes `pe.edu.upc.coldchain`. Los nuevos servicios
usan esos mismos componentes, con subpaquetes `telemetry` y `alerts`. No hay librería
de dominio compartida, dependencias Maven entre servicios ni consultas a bases ajenas.
Cada servicio es una aplicación/JAR independiente. El POM raíz solo agrega builds.

**Implementado:** Registry y esqueletos de arranque de Telemetry/Alerts; frontend de
prueba. **Solo diseñado:** endpoints de mediciones, evaluación y consulta de alertas,
reglas térmicas, clientes entre servicios y persistencia del nuevo dominio. Consultar
los contratos versionados en `docs/api-contracts/`. Los contratos no implican que las
rutas funcionales nuevas ya respondan; actualmente responden 404. Swagger muestra
las rutas realmente implementadas, no simula las operaciones futuras.

## Comunicación acordada para implementar después

1. Frontend consulta Registry en `/api/v1/monitoring-points`.
2. Un emisor envía temperatura (grados Celsius) y fecha RFC 3339 con zona a Telemetry.
3. Telemetry valida el UUID contra `GET /api/v1/monitoring-points/{id}` de Registry.
   Un punto inexistente devuelve 404; Registry no disponible, 503; no se persiste.
4. Telemetry persiste la medición con UUID propio y hace POST HTTP síncrono a
   `/api/alerts/evaluate`, enviando esa medición. Sin broker ni gateway.
5. Alerts valida el punto contra Registry y busca una regla explícita por UUID en
   su propia configuración externalizada: `coldchain.alerts.rules.<UUID>.threshold-min`
   y `threshold-max`, ambos decimales Celsius. Se prevé un archivo externo montado con
   `SPRING_CONFIG_ADDITIONAL_LOCATION` (no incluido ni cargado todavía). No se deducen
   límites de `ALMACENAMIENTO`/`TRANSPORTE`; Registry no tiene ese dato.
6. Una regla válida exige min <= max; la futura implementación debe rechazar al
   arrancar una configuración inválida, no sustituirla por valores implícitos.
   Dentro del intervalo **inclusivo**, devuelve
   `NO_ALERT`; fuera, crea una alerta `OPEN` con snapshot de ambos umbrales. Sin regla,
   devuelve 422 `THERMAL_RULE_NOT_CONFIGURED`; no se crea alerta ni se aplican defaults.

La regla es responsabilidad funcional del equipo/producto: asignar límites por punto
antes de habilitar evaluación. Un futuro CRUD de reglas o extensión de Registry queda
fuera de esta fase. La configuración no contiene secretos y no se aceptan umbrales
arbitrarios enviados por el emisor. No se implementa cierre/ack de alertas aún.

### Fallos e idempotencia (contrato, implementación pendiente)

POST measurements devuelve 201 una vez persistido, con `evaluationStatus`:
`COMPLETED`, `PENDING` (timeout/5xx de Alerts) o `RULE_NOT_CONFIGURED` (422).
No se revierte una medición guardada por un fallo posterior de evaluación. Una
evaluación pendiente debe reintentarse con la misma medición mediante el endpoint de
Alerts; el MVP no promete cola durable, scheduler, entrega automática ni exactly-once.
Los responsables de pruebas deben cubrir y hacer visible este estado, sin interpretar
201 como garantía de alerta. GET measurements expone también ese estado.
La reconciliación automática del estado en Telemetry no está resuelta por estos
endpoints: después de un reintento externo puede permanecer PENDING. No se promete
consistencia eventual automática en el MVP; acordar una estrategia antes de añadirla.

Alerts debe usar `measurementId` como clave única de idempotencia y guardar también
las evaluaciones sin alerta. Repetir el mismo payload devuelve el resultado original
(200), aun si después cambian las reglas; reutilizar ID con otros datos devuelve 409.
El timestamp de entrada es el de la medición; `createdAt` lo genera el servidor en UTC.
GET devuelve arrays, `[]` si no hay datos, ordenados por fecha ascendente y UUID como
desempate. Paginación, deduplicación de ingesta, autenticación y procesamiento asíncrono
quedan pendientes. No usar estos esqueletos como servicios públicos.

## Configuración y ejecución

| Componente | Puerto | Variables / valores locales por defecto |
|---|---:|---|
| Registry | 8080 | `PORT`, `DB_URL` (coldchain), `DB_USER` (coldchain), `DB_PASSWORD` requerida |
| Telemetry | 8081 | `PORT`, `DB_URL` (coldchain_telemetry), `DB_USER` requerida, `DB_PASSWORD` requerida, `REGISTRY_BASE_URL` (http://localhost:8080), `ALERTS_BASE_URL` (http://localhost:8082) |
| Alerts | 8082 | `PORT`, `DB_URL` (coldchain_alerts), `DB_USER` requerida, `DB_PASSWORD` requerida, `REGISTRY_BASE_URL` (http://localhost:8080) |
| Frontend | 5173 | `VITE_REGISTRY_BASE_URL` (/registry), `VITE_TELEMETRY_BASE_URL` (/telemetry), `VITE_ALERTS_BASE_URL` (/alerts) |

Cada proceso recibe su propio entorno; no exportar un único `PORT`/`DB_URL` para los tres.
PostgreSQL: crear bases y usuarios independientes previamente, sin permisos cruzados.
Los nuevos proyectos incluyen Flyway pero ninguna migración de dominio: no crean tablas
de negocio. Hibernate usa `validate`, nunca `update`. H2 solo está en el classpath de
test/smoke. Tests no requieren PostgreSQL ni servicios externos.

Health real: `/actuator/health`; liveness: `/actuator/health/liveness`; readiness:
`/actuator/health/readiness` en los nuevos servicios. Health incluye conectividad DB;
liveness solo comprueba el proceso. Swagger: `/swagger-ui.html`; API real generada:
`/v3/api-docs`. No hay credenciales predeterminadas nuevas. Logs por stdout/stderr.
El perfil `smoke` existe exclusivamente en tests: no usar en producción.

Las URLs entre servicios están externalizadas como puntos de integración; todavía no
hay clientes HTTP ni llamadas al iniciar. Al implementarlos, configurar timeouts finitos
(propuesta: conexión 2s, respuesta 5s), sin reintentos de ingesta que dupliquen mediciones.

Vite reenvía los prefijos locales a 8080/8081/8082 y los elimina; así no se modifica CORS
de Registry. En producción, el equipo debe ofrecer proxy de mismo origen o CORS explícito
si decide URLs absolutas. Las variables `VITE_*` son públicas y se fijan **al compilar**:
no poner tokens ni contraseñas en ellas. Vite dev server no es un servidor de producción.

## Trabajo paralelo del equipo

- **Docker/Compose:** usar cada carpeta de servicio como contexto, generar imágenes/JAR
  independientes (Java 17); Registry conserva sus archivos Docker anteriores en su
  carpeta. Usar DNS de Compose en URLs internas; `localhost` solo sirve fuera de Docker.
  Gestionar secretos fuera de Git, tres bases y healthchecks/readiness. El archivo raíz
  `docker-compose.yml` es un marcador vacío, no una composición ejecutable.
- **CI/CD:** el workflow previo de Registry conserva jobs, gate 70%, Checkstyle y
  artefactos; solo se adaptan rutas. Agregar luego jobs por servicio y frontend usando
  los comandos del README. No hay pipelines nuevos ni despliegues en esta fase.
- **Azure/IaC:** reservar variables, puertos, bases, secretos, probes y URL pública del
  frontend. No se crean recursos ni manifiestos. Acordar seguridad antes de exposición.
- **DevSecOps:** preparar análisis de dependencias Java/npm, escaneo de secretos y
  revisión de exposición de Swagger/Actuator y de `/api/alerts/evaluate`. No existe
  autenticación implementada; acordar controles y gestión de secretos antes de publicar.
- **Serverless:** puede diseñar un emisor de mediciones contra el POST de Telemetry
  con UUID de punto existente y fecha con zona. No hay trigger, función ni despliegue
  definidos; acordarlos con el equipo sin cambiar el contrato ni introducir un broker.
- **Testing:** Registry mantiene tests existentes; nuevos smoke tests verifican arranque,
  salud, Swagger y ausencia de endpoints de negocio. Crear aceptación con los YAML
  contractuales: UUID/fechas inválidos, punto inexistente, regla ausente, límites
  inclusivos, duplicado de evaluación, fallos HTTP y estado pendiente. Integración con
  PostgreSQL real es responsabilidad pendiente; H2 no demuestra compatibilidad total.
- **Backend:** implementar primero validaciones y persistencia contra contratos; luego
  HTTP e idempotencia. No importar entidades ni reutilizar las tablas de otro servicio.
- **Frontend:** reemplazar página de diagnóstico por vistas a partir de contratos;
  salud y URLs ya tienen cliente tipado, pero no existe dashboard ni autenticación.

## Compatibilidad de archivos anteriores

El README original y sus decisiones se conservan en
`services/monitoring-registry/README.md`, adaptando únicamente la ubicación y enlaces
de documentación. Sus referencias originales a LEEME-PRIMERO.md y
docs/evidence/ESTADO.md apuntaban a archivos inexistentes y se reemplazan por los
documentos actuales. Evidencias e HU-04 permanecen en `docs/` raíz como material
histórico, no como resultados de esta ejecución.
Los scripts/Dockerfile/compose anteriores se conservan junto a Registry. El `.env`
local anterior no se lee ni mueve: si se usa Compose legado, configurarlo localmente
en esa carpeta. No ejecutar el Compose raíz vacío ni combinar automáticamente ambos.
