# Alert Management

Java 17, Spring Boot 3.5.16, Maven Wrapper 3.9.11, PostgreSQL 16, Flyway y springdoc.
Puerto local **8082**, aplicación independiente.

## API

| Método | Ruta | Resultado |
|---|---|---|
| POST | `/api/alerts/evaluate` | 200 NO_ALERT o ALERT_CREATED; reintento idempotente |
| GET | `/api/alerts?monitoringPointId={uuid}` | 200 array por createdAt/UUID; `[]` sin alertas |
| GET | `/actuator/health` | Salud y conectividad DB |

Errores Problem Details: 400 entrada inválida, 404 punto inexistente, 409 mismo
measurementId con otros datos, 422 THERMAL_RULE_NOT_CONFIGURED, 503 Registry/DB no
disponibles. GET no llama a Registry. Repetir una evaluación ya guardada tampoco
lo consulta, ni vuelve a aplicar reglas modificadas.

Salud adicional: `/actuator/health/liveness`, `/actuator/health/readiness`.
Swagger: http://localhost:8082/swagger-ui.html; API generada: `/v3/api-docs`.
Contrato canónico: [OpenAPI 0.2.0](../../docs/api-contracts/alert-management-v1.yaml).

## Reglas por punto, sin defaults

Registry solo tiene UUID, código, nombre y tipo: **no tiene límites térmicos**.
Se implementa la configuración por UUID ya acordada, sin modificar Registry.
Crear un archivo YAML **externo** con límites elegidos por el responsable del producto.
Ejemplo ilustrativo, NO regla universal ni configuración cargada por defecto:

```yaml
coldchain:
  alerts:
    rules:
      "b60d6bd0-e058-425d-9102-434b546e1284": # Sustituir por UUID REAL de Registry
        threshold-min: 2.0                 # Valores SOLO ilustrativos de este punto
        threshold-max: 8.0
```

Indicarlo con `SPRING_CONFIG_ADDITIONAL_LOCATION=file:/ruta/absoluta/thermal-rules.yml`
(Windows: `file:C:/ruta/thermal-rules.yml`). Se recomienda ruta explícita, no `optional:`:
si se esperaba un archivo y falta, es mejor fallar al arrancar que ignorarlo.
No se incluye ningún archivo productivo de reglas en Git. No usar reglas de ejemplo
para productos reales sin validación funcional.

Reglas parciales, números no finitos, precisión no soportada o min > max impiden el
arranque. Límites iguales son válidos. Sin reglas el proceso puede arrancar, pero
evaluar un punto existente sin regla devuelve 422 y no persiste evaluación ni alerta.
El emisor nunca suministra umbrales como parte de la medición.

Comparación decimal exacta, extremos inclusivos:

- min <= temperatura <= max → NO_ALERT (se guarda evaluación para idempotencia).
- temperatura < min → alerta OPEN, type LOW_TEMPERATURE.
- temperatura > max → alerta OPEN, type HIGH_TEMPERATURE.

Alertas guardan snapshots de ambos límites; cambios posteriores no alteran resultados
anteriores. No hay cierre, reconocimiento, notificaciones ni límites universales.

## Variables y ejecución

Crear previamente una base y usuario exclusivos. Maven/Java no cargan `.env` solos.

| Variable | Default / requisito |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/coldchain_alerts` |
| `DB_USER` | Requerida |
| `DB_PASSWORD` | Requerida; sin contraseña predeterminada |
| `PORT` | `8082` |
| `REGISTRY_BASE_URL` | `http://localhost:8080` |
| `HTTP_CONNECT_TIMEOUT` | `2s`, duración positiva |
| `HTTP_READ_TIMEOUT` | `5s`, duración positiva |
| `SPRING_CONFIG_ADDITIONAL_LOCATION` | Archivo externo con reglas explícitas por punto |

Desde esta carpeta, con variables definidas:

```sh
sh mvnw clean verify
sh mvnw spring-boot:run
# Alternativa, el JAR empaquetado (sin H2):
java -jar target/alert-management-0.1.0.jar
```

En PowerShell usar `.\mvnw.cmd`. Flyway crea `alerts` y `measurement_evaluations` en
su base propia; Hibernate valida el esquema. Sin lecturas SQL ni FK a otros servicios.

## Flujo local completo

1. Arrancar Registry en 8080 con su base propia y registrar un punto mediante su API.
2. Copiar el UUID devuelto al archivo externo de reglas, con límites funcionalmente
   aprobados. Arrancar Alerts en 8082 con su propia base y ese archivo.
3. Arrancar Telemetry en 8081 con su base y `ALERTS_BASE_URL=http://localhost:8082`.
4. Enviar medición a Telemetry (NO directamente a las tablas), con el UUID real.
5. Telemetry devuelve 201 COMPLETED cuando Alerts responde correctamente, tanto si
   hubo alerta como si no. Consultar este servicio para comprobar las alertas.

Ejemplo directo de evaluación con UUID ilustrativos:

```sh
curl -i -X POST http://localhost:8082/api/alerts/evaluate \
  -H 'Content-Type: application/json' \
  -d '{"measurementId":"0a41469b-9a1d-4245-a360-29986b37f44b","monitoringPointId":"b60d6bd0-e058-425d-9102-434b546e1284","temperature":9,"timestamp":"2026-10-29T10:00:00Z"}'
```

Respuesta ilustrativa con la regla de ejemplo, HTTP 200:

```json
{
  "measurementId": "0a41469b-9a1d-4245-a360-29986b37f44b",
  "outcome": "ALERT_CREATED",
  "alert": {
    "id": "c6423de0-8fae-4f15-bd77-a3e5b87ab9d6",
    "monitoringPointId": "b60d6bd0-e058-425d-9102-434b546e1284",
    "measurementId": "0a41469b-9a1d-4245-a360-29986b37f44b",
    "temperature": 9,
    "thresholdMin": 2,
    "thresholdMax": 8,
    "status": "OPEN",
    "createdAt": "2026-10-29T10:00:01Z",
    "type": "HIGH_TEMPERATURE"
  }
}
```

Dentro del intervalo: `{"measurementId":"...","outcome":"NO_ALERT"}`, sin `alert`.
GET devuelve solo el objeto interno de alerta como elementos del array:

```sh
curl 'http://localhost:8082/api/alerts?monitoringPointId=b60d6bd0-e058-425d-9102-434b546e1284'
```

## Fallos y reintentos

Telemetry confirma la medición **antes** de llamar a Alerts. Si Alerts está caído o
la llamada expira, Telemetry devuelve 201 PENDING, sin revertir ni duplicar la medición.
No hay reintentos automáticos. Para recuperar, repetir POST `/api/alerts/evaluate`
con el measurementId, punto, temperatura y timestamp originales. No reenviar POST
measurements para reintentar evaluación, pues la ingesta no es idempotente.

Repetir evaluación produce el resultado original, incluso con reglas nuevas, sin
crear alertas adicionales. Distintos datos con la misma medición → 409, también bajo
concurrencia. Se normaliza escala decimal y fecha a UTC/microsegundos al comparar.
Si hubo timeout después del commit de Alerts, el reintento igualmente es seguro.

Limitación conservada: el reintento manual no reconcilia automáticamente el PENDING
de Telemetry. No hay cola, outbox ni scheduler, y no se promete exactly-once.
No publicar el endpoint interno sin los controles acordados con DevSecOps.

## Pruebas

```sh
sh mvnw clean verify
```

Por defecto usa H2, migración real, Tomcat y un doble HTTP local de Registry. Incluye
unitarias, validación de configuración al arrancar, límites inclusivos, precisión,
consultas, persistencia de NO_ALERT, duplicados secuenciales y concurrentes, conflictos,
reglas ausentes y fallos HTTP/DB. Reportes en `target/surefire-reports/`.

Para repetir contra PostgreSQL, ejecutar **solo este proyecto** sobre una base de
pruebas exclusiva y desechable; las pruebas borran sus alertas/evaluaciones:

```sh
export TEST_DB_URL='jdbc:postgresql://localhost:5432/coldchain_alerts_test'
export TEST_DB_USER='USUARIO_DE_PRUEBA'
read -r -s TEST_DB_PASSWORD
export TEST_DB_PASSWORD
sh mvnw clean verify
```

Nunca usar una base compartida/productiva. No ejecutar el agregador con un mismo
TEST_DB_URL para Telemetry y Alerts: cada servicio tiene su propia migración V1 y base.
Sin TEST_DB_URL se usa H2. El perfil `smoke` está solo en test, no en el JAR productivo.

Demo H2 manual: `sh mvnw spring-boot:test-run -Dspring-boot.run.profiles=smoke`;
sin Registry no evalúa mediciones nuevas y sin reglas devuelve 422 para puntos existentes.
Decisiones, archivos y resultados: [documentación](../../docs/architecture/alert-management.md).
