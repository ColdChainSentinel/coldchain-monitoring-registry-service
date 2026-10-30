# Contratos API

- `telemetry-ingestion-v1.yaml`: API implementada de ingesta/consulta, puerto 8081; revisión 0.1.1.
- `alert-management-v1.yaml`: API implementada de evaluación/consulta, puerto 8082; revisión 0.2.0.
- Registry: API implementada en `/v3/api-docs`, puerto 8080. Rutas reales:
  `/api/v1/monitoring-points` y `/api/v1/monitoring-points/{id}`. UUID como identificador;
  DTO: `id`, `code`, `name`, `type` (`ALMACENAMIENTO` o `TRANSPORTE`). Sin límites térmicos.

OpenAPI 3.0.3. Alerts 0.2.0 añade `type` opcional de solo lectura a las alertas,
sin modificar rutas, campos obligatorios ni los resultados de evaluación existentes.
Reglas explícitas e idempotencia: [decisiones de Alerts](../architecture/alert-management.md).
Telemetry 0.1.1 conserva rutas, campos y estados; precisa validación JSON y timestamp
UTC a microsegundos. Ver [decisiones](../architecture/telemetry-ingestion.md).
Cambios incompatibles requieren nueva versión de contrato y acuerdo del equipo.
Ver [guía de integración](../architecture/integration-guide.md) para reglas, errores,
idempotencia, estados pendientes y tareas de implementación. Temperaturas: Celsius;
Java previsto: UUID, BigDecimal e Instant. No se comparten entidades JPA.
