# Telemetry Ingestion — esqueleto

Java 17 / Spring Boot 3.5.16, Maven Wrapper 3.9.11. Puerto 8081.
Capas previstas: `controller`, `dto`, `service`, `repository`, `model`, `exception`
bajo `pe.edu.upc.coldchain.telemetry`, siguiendo Registry cuando se implemente negocio.

- Compilar/probar: `sh mvnw clean verify` (Windows: `.\mvnw.cmd clean verify`).
- Ejecutar: definir `DB_URL`, `DB_USER`, `DB_PASSWORD`; `sh mvnw spring-boot:run`.
- Demo sin PostgreSQL: `sh mvnw spring-boot:test-run -Dspring-boot.run.profiles=smoke`.
  Usa H2 exclusivamente desde test; no representa un despliegue productivo.
- Salud: http://localhost:8081/actuator/health
- Swagger: http://localhost:8081/swagger-ui.html
- JAR: `target/telemetry-ingestion-0.1.0.jar` (no contiene H2).

Las URLs `REGISTRY_BASE_URL`, `ALERTS_BASE_URL` reservan configuración para futuros
clientes; no se realizan llamadas aún. Sin tablas de mediciones ni endpoints de negocio.
Contrato: [telemetry-ingestion-v1.yaml](../../docs/api-contracts/telemetry-ingestion-v1.yaml).
Consultar [guía](../../docs/architecture/integration-guide.md) antes de integrar.
