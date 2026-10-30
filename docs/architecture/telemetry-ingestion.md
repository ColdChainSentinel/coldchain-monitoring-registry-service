# Telemetry Ingestion — decisiones de implementación

> Este documento conserva los resultados de la fase de Telemetry, cuando Alerts aún
> era un esqueleto. Para la implementación posterior y el flujo real completo,
> consultar [Alert Management](alert-management.md).

## Compatibilidad (acordada antes de implementar)

Se conservan rutas, códigos de respuesta, campos, UUID y estados de la versión 0.1.0.
La revisión 0.1.1 del contrato precisa representación, sin añadir campos obligatorios:

- `temperature`: número JSON finito en Celsius, Java `BigDecimal`, PostgreSQL `NUMERIC`
  **sin escala fija ni redondeo**; los ceros finales/exponentes se normalizan sin alterar
  el valor. Negativos y cero son válidos: no hay reglas térmicas
  en Telemetry. Se rechazan strings, booleanos y números fuera de la capacidad técnica
  de NUMERIC (131072 dígitos enteros / 16383 decimales), no por límites de producto.
- `timestamp`: string RFC 3339 con zona explícita, años 0001–9999 y hasta nueve
  decimales de segundo; se normaliza a UTC y se **trunca a microsegundos** antes de
  guardar/responder/enviar a Alerts, evitando discrepancias con PostgreSQL. No se
  prohíben fechas futuras: el contrato no define una política de reloj del emisor.
- UUID de cuerpo y query: representación canónica de 36 caracteres con guiones;
  mayúsculas aceptadas, respuesta normalizada. No se aceptan formas abreviadas/base64.
- JSON inválido, campos nulos/ausentes o tipos incompatibles: 400 Problem Details.
  Propiedades desconocidas se ignoran, siguiendo el comportamiento inicial de Registry.
- GET devuelve array por `timestamp` ascendente y UUID como desempate; no consulta
  Registry y no pagina, tal como el contrato inicial.

Estas precisiones deben conocerlas los emisores serverless y el equipo de testing;
en particular, no comparar literalmente un timestamp de entrada nanosegundo con la
respuesta ni enviar números/fechas como strings o epoch numérico.

## Flujo y fallos

1. Validación HTTP antes de acceder a dependencias.
2. GET a Registry para verificar que el punto existe. 404 → 404 local, errores de red,
   timeout, otras respuestas inesperadas o respuesta inválida → 503, sin inserción.
3. Guardar y **confirmar** una medición PENDING en la base propia de Telemetry.
4. POST HTTP a Alerts con esa medición ya persistida, fuera de la transacción de DB.
5. 200 con resultado válido → COMPLETED; 422 con código
   THERMAL_RULE_NOT_CONFIGURED → RULE_NOT_CONFIGURED; cualquier otro fallo o respuesta
   inválida → PENDING. No se crean alertas desde este servicio.
6. Guardar el resultado en una segunda transacción y responder 201 con el último estado
   confirmado. Si falla solo esta actualización, devolver conservadoramente
   PENDING, sin negar una inserción ya confirmada. Un fallo ambiguo durante commit
   podría haber confirmado la actualización; un GET posterior es la fuente de verdad.
   Los fallos de Alerts se registran
   sin incluir payloads, credenciales ni respuestas externas.

Sin reintentos automáticos, cola ni scheduler. La integración con Alerts puede fallar
y no bloquea el almacenamiento. No hay deduplicación de POST: no reenviar sin evaluar
el riesgo de duplicados. La reconciliación de PENDING sigue fuera de alcance.

Conexión HTTP: 2s; lectura: 5s por defecto, externalizadas. No se mantiene una
transacción abierta esperando HTTP. Registry y Alerts no participan en readiness:
las consultas de mediciones almacenadas deben funcionar si aquellos no están disponibles.

## Alcance de cambios

Solo código/tests/configuración propios de Telemetry y documentación/contrato asociado.
No se modifican Registry, Alerts, frontend, infraestructura, workflows ni tests compartidos.
Flyway crea `measurements` e índice `(monitoring_point_id, timestamp, id)`. No existe
foreign key a Registry ni acceso a sus tablas. Hibernate valida el esquema.

### Archivos del servicio

Bajo `services/telemetry-ingestion/src/main/java/pe/edu/upc/coldchain/telemetry/`:

```text
TelemetryApplication.java                 # Metadatos OpenAPI actualizados
controller/MeasurementController.java
dto/CreateMeasurementRequest.java
dto/MeasurementResponse.java
model/Measurement.java
model/EvaluationStatus.java
repository/MeasurementRepository.java
service/MeasurementService.java            # Orquestación, sin transacción de red
service/MeasurementStore.java              # Transacciones y consultas
client/RegistryClient.java
client/AlertsClient.java                   # Solo cliente; no implementa Alerts
config/ClientsConfiguration.java
config/ClientsProperties.java
config/MeasurementJsonConfiguration.java
validation/PostgresNumeric.java
validation/PostgresNumericValidator.java
exception/ApiExceptionHandler.java
exception/MonitoringPointNotFoundException.java
exception/RegistryUnavailableException.java
```

Otros cambios propios:

- `src/main/resources/application.yml`: timeouts HTTP externalizados.
- `src/main/resources/db/migration/V1__create_measurements.sql`: nueva tabla/índice.
- `src/test/resources/application-smoke.yml`: admite TEST_DB_* sin tocar configuración productiva.
- `src/test/java/.../TelemetryApplicationTest.java`: conserva salud/Swagger, actualiza
  expectativa de la ruta de negocio (query requerida, no 404).
- Nuevas clases de prueba en el mismo paquete: `MeasurementServiceTest`,
  `MeasurementStoreTest`, `PostgresNumericValidatorTest`, `RegistryClientTest`,
  `AlertsClientTest`, `MeasurementRepositoryTest`, `MeasurementApiTest` y
  `MeasurementFailureApiTest`.
- README del servicio: ejecución, ejemplos, variables y pruebas PostgreSQL.

Documentación compartida actualizada solo para reflejar Telemetry: README raíz,
`docs/api-contracts/README.md`, contrato Telemetry, guía de integración y este documento.
No se añadieron dependencias ni se modificó ningún POM.

## Verificación ejecutada

29 de octubre de 2026, Java 17.0.21 / Maven Wrapper 3.9.11, entorno Linux/WSL.

| Comprobación | Resultado real |
|---|---|
| Telemetry `sh mvnw -B -ntp clean verify`, H2 | **93 tests**, 0 fallos/errores/omitidos; BUILD SUCCESS |
| Misma suite, TEST_DB_* contra PostgreSQL **16.6** real | **93 tests**, 0 fallos/errores/omitidos; BUILD SUCCESS |
| Registry `sh mvnw -B -ntp verify` | **20 tests**, 0 fallos/errores/omitidos; Checkstyle y gate JaCoCo aprobados |
| Telemetry con configuración Checkstyle existente de Registry | 0 violaciones, BUILD SUCCESS; sin modificar plugins/workflows |
| Contrato Telemetry, swagger-parser 12.1.0 | OpenAPI 3.0.3 válido, revisión API 0.1.1 |
| Documento generado `/v3/api-docs` | OpenAPI 3.1.0 válido, revisión API 0.1.1 |
| Inspección del JAR | Sin H2 ni `application-smoke.yml` empaquetados |

Se usó un clúster PostgreSQL temporal, aislado y limitado a loopback. Ninguna base
existente del usuario ni configuración del sistema fue alterada. No se usó Docker,
no se realizaron despliegues, commits, push ni cambios remotos.

### Comprobación con JAR productivo y Registry real

Se ejecutaron los JAR de Registry y Telemetry **sin perfiles de test**, con bases
PostgreSQL diferentes en el clúster temporal y puertos locales 18080/18081. Resultado:

- Ambos `/actuator/health`: **200 UP**.
- Registrar punto en Registry y enviar su UUID a Telemetry: **201 PENDING**, porque
  no había proceso Alerts escuchando en la URL configurada.
- GET: **200** con la medición guardada; consulta de otro UUID: **200 []**.
- Temperatura string: **400**; UUID de punto inexistente en Registry: **404**.
- Swagger y API generada: **200**.
- Reiniciar el JAR de Telemetry: la misma medición sigue disponible, con mismo UUID,
  decimal, fecha y estado; Flyway valida la migración existente sin cambios destructivos.
- Detener Registry: nuevo POST **503**, GET de la medición **200**, salud **200 UP**;
  no se inserta una medición adicional.

Los procesos Java y el clúster PostgreSQL temporal se detuvieron al terminar.
Los 93 casos son la misma suite ejecutada contra dos motores, no 186 casos distintos.
Incluyen control de exponentes extremos para devolver 400, no errores internos, y
prueba de visibilidad de la inserción confirmada desde el doble HTTP de Alerts.
Se encontró y corrigió aceptación de UUID abreviados en query por conversión de
Spring; la prueba de regresión ahora pasa.

### Pendientes y límites

- Alerts real no implementado ni validado: COMPLETED y RULE_NOT_CONFIGURED se prueban
  con dobles HTTP; el camino PENDING también se probó con conexión real rechazada.
- No hay autenticación, rate limiting, paginación, retención, reintentos ni recuperación
  automática de PENDING. No exponer como API pública sin el trabajo de DevSecOps.
- Validar punto en Registry y guardar en Telemetry no es una transacción distribuida;
  un futuro borrado concurrente de puntos requerirá política funcional explícita.
- Fallos ambiguos de conexión/commit pueden requerir consulta y reconciliación. No se
  promete exactly-once ni idempotencia de ingesta; POST repetido puede crear duplicados.
- No se ejecutaron pipelines remotos, pruebas de carga, frontend E2E ni Azure. Los
  responsables de CI pueden reutilizar los comandos sin cambiar contratos de infraestructura.
