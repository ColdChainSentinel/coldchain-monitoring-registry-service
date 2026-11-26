# Contratos entre microservicios (Pact)

Pruebas de contrato dirigidas por el consumidor (*consumer-driven contracts*):

| Consumidor | Proveedor | Interacciones | Prueba del consumidor | Verificación del proveedor |
|---|---|---|---|---|
| telemetry-ingestion | monitoring-registry | punto existente (200), punto inexistente (404) | `TelemetryConsumerPactTest` | `RegistryProviderPactTest` |
| telemetry-ingestion | alert-management | fuera de rango (ALERT_CREATED), dentro del rango (NO_ALERT), sin regla (422 problem+json) | `TelemetryConsumerPactTest` | `AlertsProviderPactTest` |

1. El consumidor ejecuta su cliente HTTP real contra un *mock server* de Pact y, si pasa,
   escribe el contrato en `contracts/pacts/`.
2. Cada proveedor levanta su API real (Spring Boot, puerto aleatorio, H2) y reproduce las
   interacciones; los *provider states* crean los datos que cada caso necesita.
3. Ambos lados corren dentro de `mvn verify`, así que el pipeline de CI falla si uno de los
   dos rompe el contrato.

```powershell
./mvnw -pl services/telemetry-ingestion test -Dtest=TelemetryConsumerPactTest
./mvnw -pl services/monitoring-registry,services/alert-management test -Dtest='*ProviderPactTest'
```
