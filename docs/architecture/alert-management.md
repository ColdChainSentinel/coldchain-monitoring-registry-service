# Alert Management — decisiones de implementación

## Contrato y configuración (documentados antes de implementar)

Registry conserva `id: UUID`, `code`, `name`, `type`. No tiene límites térmicos ni
reglas por producto. No se modificará su modelo ni su base. Se implementa la fuente
de reglas explícitas por UUID que ya estaba acordada en la guía de integración:
`coldchain.alerts.rules.<UUID>.threshold-min` / `threshold-max` en configuración externa.
No hay reglas predeterminadas; punto existente sin regla → 422
`THERMAL_RULE_NOT_CONFIGURED`. Ambos límites son obligatorios, finitos y min <= max;
una configuración inválida impide arrancar. Iguales entre sí están permitidos.

El contrato de Alerts pasa de 0.1.0 a **0.2.0** sin cambiar rutas, campos obligatorios,
estados OPEN ni resultados NO_ALERT/ALERT_CREATED. Se añade el campo **opcional y de
solo lectura** `type` a Alert: LOW_TEMPERATURE / HIGH_TEMPERATURE. Es una ampliación
compatible: Telemetry ya ignora propiedades desconocidas y no necesita cambiar su
cliente ni sus DTO. Los consumidores antiguos pueden ignorar el campo nuevo.

La comparación usa BigDecimal, no double: min <= temperature <= max → NO_ALERT;
por debajo → LOW_TEMPERATURE; por encima → HIGH_TEMPERATURE. Cada alerta guarda
snapshot de temperatura y ambos umbrales. No se admiten límites enviados por el emisor.

Las entradas mantienen las precisiones acordadas con Telemetry: UUID canónico,
números JSON (no strings), fecha RFC 3339 con zona; UTC truncado a microsegundos,
NUMERIC sin escala fija ni redondeo. Exponentes fuera de la capacidad técnica de
PostgreSQL se rechazan con 400, no con un umbral térmico inventado.

## Transacciones e idempotencia

1. Validar la entrada y normalizar fecha/decimal.
2. Buscar evaluación por `measurementId` **antes** de consultar Registry o reglas.
   Si existe, comparar punto, temperatura por valor y timestamp normalizado: mismo
   contenido devuelve el resultado guardado; distinto contenido devuelve 409.
3. Para una medición nueva, consultar Registry fuera de la transacción: inexistente
   → 404; fallo/timeout/respuesta inválida → 503. Validar regla explícita o devolver 422.
4. Insertar evaluación y, si corresponde, alerta en una transacción local. También
   se guardan NO_ALERT, para que los reintentos no reevalúen con reglas distintas.
5. La PK de evaluación es measurementId; la alerta también tiene measurementId único.
   La inserción es explícita (no merge/upsert que pueda sobrescribir una evaluación).
   Ante colisión concurrente, la transacción perdedora revierte; en una nueva lectura
   se devuelve el resultado ganador o 409. Sin alertas huérfanas tras rollback.

Cada servicio usa su propia base, sin FK ni lectura SQL entre microservicios. No se
mantiene una transacción abierta mientras se llama a Registry. GET alerts solo usa
la base local; array ordenado por createdAt ascendente y UUID, `[]` sin resultados.

## Integración con Telemetry y límites del MVP

Telemetry ya guarda/confirma la medición y llama a `/api/alerts/evaluate` con timeout.
200 válido → COMPLETED; 422 con código esperado → RULE_NOT_CONFIGURED; demás fallos
→ PENDING. No se reenvía POST measurements al fallar Alerts: eso crearía otra medición.
Para recuperar una evaluación fallida se puede repetir **POST alerts/evaluate** con
el measurementId y contenido original; es idempotente incluso tras reinicio/cambio
de reglas o si Registry está temporalmente caído (para una evaluación ya almacenada).

No hay reintentos automáticos, scheduler, broker, notificaciones ni cierre de alertas.
Un reintento manual exitoso no actualiza automáticamente el estado PENDING guardado
en Telemetry; se mantiene la limitación ya documentada. No se promete exactly-once,
idempotencia de ingesta ni transacción distribuida. Un cliente no debe inferir que
201 de Telemetry garantiza la creación de alerta. El endpoint de evaluación es interno,
sin autenticación aún: no exponer públicamente.

## Archivos y responsabilidades

En `services/alert-management/src/main/java/pe/edu/upc/coldchain/alerts/`:

- `controller/AlertController.java`: evaluar y consultar.
- `dto/EvaluateMeasurementRequest.java`, `EvaluationResponse.java`, `AlertResponse.java`.
- `model/MeasurementEvaluation.java`, `Alert.java`, `EvaluationOutcome.java`,
  `AlertType.java`, `AlertStatus.java`.
- `repository/EvaluationRepository.java`, `AlertRepository.java`.
- `service/ThermalEvaluator.java`: comparación decimal inclusiva.
- `service/EvaluationService.java`: idempotencia y orquestación HTTP fuera de transacción.
- `service/EvaluationStore.java`: inserción explícita, transacciones y consultas.
- `client/RegistryClient.java`: validación del punto por la API existente.
- `config/AlertsConfiguration.java`, `RegistryProperties.java`, `ThermalRule.java`,
  `ThermalRulesProperties.java`, `AlertJsonConfiguration.java`.
- `validation/PostgresNumeric.java`, `PostgresNumericValidator.java`: capacidad técnica
  de almacenamiento, no límites térmicos. Se mantiene implementación local para no
  introducir dependencia de compilación con Telemetry ni una librería compartida.
- `exception/ApiExceptionHandler.java`, `MeasurementConflictException.java`,
  `ThermalRuleNotConfiguredException.java`, `MonitoringPointNotFoundException.java`,
  `RegistryUnavailableException.java`.
- `AlertsApplication.java`: metadatos OpenAPI 0.2.0.

Otros cambios de Alerts: README, `src/main/resources/application.yml`, nueva migración
`db/migration/V1__create_alerts_and_evaluations.sql`, perfil test `application-smoke.yml`
y actualización de la prueba de arranque existente. Nuevas pruebas: `ThermalEvaluatorTest`,
`ThermalRulesPropertiesTest`, `EvaluationServiceTest`, `EvaluationPersistenceTest`,
`RegistryClientTest`, `AlertApiTest`, `AlertFailureApiTest`.

Solo documentación compartida adicional: README raíz, índice de contratos, contrato
Alerts, guía de integración y nota histórica en el documento de Telemetry. No se
modifican fuentes/configuraciones/tests de Telemetry ni Registry, frontend, infraestructura,
Docker, workflows, POM ni tests compartidos. Los cambios previos de Telemetry se conservan.

## Verificación ejecutada

29 de octubre de 2026 (hora local WSL), Temurin 17.0.21 y Maven Wrapper 3.9.11.

| Verificación | Resultado real |
|---|---|
| Alerts `sh mvnw -B -ntp clean verify`, H2 | **93 tests**, 0 fallos/errores/omitidos; BUILD SUCCESS |
| Misma suite con TEST_DB_* en PostgreSQL **16.6** real | **93 tests**, 0 fallos/errores/omitidos; BUILD SUCCESS |
| Regresión Telemetry | **93 tests**, 0 fallos/errores/omitidos; BUILD SUCCESS |
| Regresión Registry | **20 tests**, 0 fallos/errores/omitidos; BUILD SUCCESS, Checkstyle y gate JaCoCo aprobados |
| Alerts con configuración Checkstyle existente de Registry | 0 violaciones, sin cambiar plugins ni workflows |
| Contrato canónico validado con swagger-parser 12.1.0 | OpenAPI 3.0.3 válido, API 0.2.0 |
| Documento generado por el JAR validado con swagger-parser 12.1.0 | OpenAPI 3.1.0 válido, API 0.2.0 |
| Inspección del JAR | Sin H2 ni configuración del perfil smoke empaquetados |

La suite Alerts se repitió contra dos motores: son 93 casos distintos, no 186.
Los casos concurrentes fuerzan solicitudes simultáneas antes de insertar y verifican
un único resultado, 409 para contenido diferente y ausencia de alertas huérfanas.
Las advertencias de configuración inválida y de colisiones SQL son intencionales
en esos casos y se comprueba que se manejan, sin fallos de tests.

### Flujo real de los tres JAR con PostgreSQL

Se arrancaron los tres JAR **sin perfiles de test**, con bases diferentes en un clúster
PostgreSQL temporal limitado a loopback. Se registraron puntos reales mediante la API
de Registry y se escribió un archivo externo de reglas solo para esos puntos de prueba.
No se usaron mocks en este flujo ni se inició el frontend.

1. Punto de prueba con regla explícita 2–8: mediciones 5, 1, 9, 2 y 8 devolvieron
   **201 COMPLETED** en Telemetry. Solo 1 y 9 generaron alertas LOW/HIGH; ambas tenían
   measurementId correcto, snapshot 2–8 y status OPEN.
2. Segundo punto con regla explícita -25–-15: -20 sin alerta, -14 con alerta HIGH;
   ambas mediciones **201 COMPLETED**. Esto verifica reglas por UUID, no universales.
3. Tercer punto sin regla: Telemetry **201 RULE_NOT_CONFIGURED**, sin alerta.
4. Repetir evaluación devuelve el mismo JSON/UUID; cambiar temperatura con el mismo
   measurementId devuelve **409**, sin duplicados.
5. Detener Alerts e ingerir otra medición: **201 PENDING**, medición guardada.
6. Reiniciar Alerts con base persistida y regla cambiada a 4–20: las evaluaciones
   previas conservan su resultado original, incluyendo una NO_ALERT que bajo la nueva
   regla sí estaría fuera de rango y una alerta que ahora estaría dentro del rango.
7. Reintentar la evaluación pendiente: se crea exactamente una alerta con snapshot
   4–20; repetirla no duplica nada. Telemetry conserva seis mediciones para ese punto,
   no siete; el estado de esa medición sigue PENDING, limitación documentada.
8. Detener Registry: replays y GET alerts **200**; nuevas evaluaciones e ingestas
   **503**. No se corrompen los resultados almacenados.
9. Salud y `/v3/api-docs` de los tres JAR respondieron **200**; Swagger de Alerts también.

Al finalizar se detuvieron los procesos Java propios y el clúster PostgreSQL temporal.
No se hicieron commits, push, despliegues ni cambios remotos. La comparación de hashes
contra el estado inicial de esta fase confirmó que las fuentes, configuración y tests
de Registry/Telemetry, frontend, infraestructura, workflows y pruebas compartidas
permanecen intactos, incluyendo los cambios de Telemetry todavía sin commit.

Los límites usados arriba son exclusivamente fixtures de puntos desechables para
demostración, no parámetros de producto ni configuración predeterminada.

## Trabajo pendiente de integración

- Docker/Compose: imágenes propias, bases/usuarios separados, URLs DNS entre servicios,
  montaje del archivo externo de reglas y sus variables. No se modificó composición.
- CI/CD: incorporar build/tests por servicio; PostgreSQL opcional con bases exclusivas,
  nunca TEST_DB_URL compartida. No se ejecutaron pipelines remotos.
- Azure/IaC: aprovisionar bases y secretos, configuración externa, probes y red privada.
  No se crearon recursos ni se hicieron despliegues.
- Responsable funcional: aprobar y asignar límites por UUID antes de habilitar cada punto.
- DevSecOps: asegurar APIs internas, Swagger/Actuator y secretos antes de exposición.
- Persisten los límites del MVP: sin autenticación avanzada, retención/paginación,
  reconciliación automática, pruebas de carga, notificaciones ni frontend definitivo.
