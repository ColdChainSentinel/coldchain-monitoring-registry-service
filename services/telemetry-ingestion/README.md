# Telemetry Ingestion

Java 17, Spring Boot 3.5.16, Maven Wrapper 3.9.11, PostgreSQL 16 y Flyway.
Puerto local **8081**. No depende de compilar otro componente.

## API implementada

| Método | Ruta | Respuestas |
|---|---|---|
| POST | `/api/measurements` | 201 guardada; 400 inválida; 404 punto inexistente; 503 Registry/DB no disponibles |
| GET | `/api/measurements?monitoringPointId={uuid}` | 200 array ordenado por timestamp/UUID, `[]` sin datos; 400 query inválida; 503 DB no disponible |
| GET | `/actuator/health` | Salud y base de datos |

También conserva `/actuator/health/liveness`, `/actuator/health/readiness`,
`/swagger-ui.html` y `/v3/api-docs`.

POST verifica el punto en Registry, confirma una medición PENDING en su propia DB y
solicita evaluación a Alerts por HTTP, sin transacciones abiertas durante la red.
Alerts no disponible → **201 PENDING**, sin perder la medición. El cliente HTTP no
implementa Alert Management. GET no llama a servicios externos.

## Ejecutar con PostgreSQL

Crear una base `coldchain_telemetry` y un usuario con permisos sobre ella. Definir por
proceso las variables (Maven no carga `.env` automáticamente):

| Variable | Default / requisito |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/coldchain_telemetry` |
| `DB_USER` | Requerida |
| `DB_PASSWORD` | Requerida, sin credencial predeterminada |
| `PORT` | `8081` |
| `REGISTRY_BASE_URL` | `http://localhost:8080` |
| `ALERTS_BASE_URL` | `http://localhost:8082` |
| `HTTP_CONNECT_TIMEOUT` | `2s`, duración positiva |
| `HTTP_READ_TIMEOUT` | `5s`, duración positiva |

Desde esta carpeta, con variables definidas:

```sh
sh mvnw clean verify
sh mvnw spring-boot:run
# O ejecutar el artefacto compilado:
java -jar target/telemetry-ingestion-0.1.0.jar
```

PowerShell: sustituir `sh mvnw` por `.\mvnw.cmd` y definir `$env:VARIABLE`.
Flyway crea `measurements` e índice compuesto; Hibernate usa `validate`, nunca `update`.
No hay claves foráneas ni acceso a la DB de Registry. El JAR no contiene H2.
Registry debe estar accesible para POST, no para consultar mediciones ya guardadas.
No se requiere iniciar Alerts para probar ingesta: quedará PENDING.

## Ejemplos

Primero obtener un UUID real con `GET http://localhost:8080/api/v1/monitoring-points`
o registrar un punto en Registry. Sustituir el UUID ilustrativo:

```sh
curl -i -X POST http://localhost:8081/api/measurements \
  -H 'Content-Type: application/json' \
  -d '{"monitoringPointId":"b60d6bd0-e058-425d-9102-434b546e1284","temperature":3.75,"timestamp":"2026-10-29T10:00:00Z"}'
```

Respuesta ilustrativa 201 si Alerts no está disponible:

```json
{
  "id": "ced027f0-b307-4538-a485-5303df89119a",
  "monitoringPointId": "b60d6bd0-e058-425d-9102-434b546e1284",
  "temperature": 3.75,
  "timestamp": "2026-10-29T10:00:00Z",
  "evaluationStatus": "PENDING"
}
```

```sh
curl 'http://localhost:8081/api/measurements?monitoringPointId=b60d6bd0-e058-425d-9102-434b546e1284'
curl http://localhost:8081/actuator/health
```

GET devuelve un array con el mismo modelo. POST `{}` devuelve Problem Details 400 con
`fields.monitoringPointId`, `fields.temperature`, `fields.timestamp`. Los fallos no
exponen detalles de conexión ni respuestas externas.

## Validaciones y contrato

Contrato [OpenAPI 0.1.1](../../docs/api-contracts/telemetry-ingestion-v1.yaml).
UUID canónico, temperatura como número JSON finito (no string) y fecha RFC 3339 con zona.
Temperaturas negativas/cero son válidas; no hay umbrales térmicos de producto aquí.
NUMERIC sin escala fija conserva decimales; se valida su capacidad técnica antes de
guardar. La fecha se normaliza a UTC y trunca a microsegundos; hasta nueve decimales
de entrada, años 0001–9999. No se rechazan fechas futuras por una política inventada.

Estados: COMPLETED (evaluación válida, con o sin alerta), RULE_NOT_CONFIGURED (422
con código explícito) y PENDING (fallo/timeout/respuesta inválida). No existen cola,
reintentos, deduplicación de ingesta ni reconciliación automática de estados.
Detalles de compatibilidad y transacciones:
[decisiones de implementación](../../docs/architecture/telemetry-ingestion.md).

## Pruebas

```sh
sh mvnw clean verify
```

Sin servicios externos: unitarias Mockito y clientes HTTP con servidor simulado;
integración HTTP con Tomcat real, dobles HTTP locales, migración Flyway y persistencia
H2 en modo PostgreSQL. Incluyen errores, campos inválidos, orden, consultas vacías,
precisión, estados de evaluación y commit antes de llamar a Alerts.
Reportes: `target/surefire-reports/`.

Para repetir **la misma suite con PostgreSQL real**, configurar una base **exclusiva,
desechable y vacía**. Las pruebas borran mediciones de esa base; nunca usar producción:

```sh
export TEST_DB_URL='jdbc:postgresql://localhost:5432/coldchain_telemetry_test'
export TEST_DB_USER='USUARIO_DE_PRUEBA'
read -r -s TEST_DB_PASSWORD
export TEST_DB_PASSWORD
sh mvnw clean verify
```

El perfil `smoke` admite únicamente estas variables `TEST_DB_*` para tests; las de
producción son `DB_*`. Sin TEST_DB_URL usa H2. No dejar TEST_DB_* apuntando a bases
compartidas al ejecutar la suite. Un test correcto con H2 no sustituye PostgreSQL.

Demo H2 manual, sin variables de test apuntando a PostgreSQL:

```sh
sh mvnw spring-boot:test-run -Dspring-boot.run.profiles=smoke
```

Sin Registry, POST devuelve 503; GET y salud funcionan. No es un perfil productivo.
No exponer la API públicamente: autenticación y límites operativos siguen pendientes.
