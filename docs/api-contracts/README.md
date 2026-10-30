# Contratos API

- `telemetry-ingestion-v1.yaml`: API prevista de ingesta/consulta, puerto 8081.
- `alert-management-v1.yaml`: API prevista de evaluación/consulta, puerto 8082.
- Registry: API implementada en `/v3/api-docs`, puerto 8080. Rutas reales:
  `/api/v1/monitoring-points` y `/api/v1/monitoring-points/{id}`. UUID como identificador;
  DTO: `id`, `code`, `name`, `type` (`ALMACENAMIENTO` o `TRANSPORTE`). Sin límites térmicos.

Versiones iniciales 0.1.0 de diseño, OpenAPI 3.0.3. No son implementación de negocio.
Cambios incompatibles requieren nueva versión de contrato y acuerdo del equipo.
Ver [guía de integración](../architecture/integration-guide.md) para reglas, errores,
idempotencia, estados pendientes y tareas de implementación. Temperaturas: Celsius;
Java previsto: UUID, BigDecimal e Instant. No se comparten entidades JPA.
