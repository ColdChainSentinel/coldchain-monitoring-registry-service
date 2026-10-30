# Alert Management — esqueleto

Java 17 / Spring Boot 3.5.16, Maven Wrapper 3.9.11. Puerto 8082.
Capas previstas: `controller`, `dto`, `service`, `repository`, `model`, `exception`
bajo `pe.edu.upc.coldchain.alerts`, siguiendo Registry al implementar negocio.

- Compilar/probar: `sh mvnw clean verify` (Windows: `.\mvnw.cmd clean verify`).
- Ejecutar: definir `DB_URL`, `DB_USER`, `DB_PASSWORD`; `sh mvnw spring-boot:run`.
- Demo sin PostgreSQL: `sh mvnw spring-boot:test-run -Dspring-boot.run.profiles=smoke`.
- Salud: http://localhost:8082/actuator/health
- Swagger: http://localhost:8082/swagger-ui.html
- JAR: `target/alert-management-0.1.0.jar` (no contiene H2).

`REGISTRY_BASE_URL` reserva configuración del futuro cliente. Sin entidades, reglas
implementadas, migraciones de dominio ni endpoints funcionales. No hay umbrales
predeterminados: el diseño exige regla explícita por UUID del punto; consultar
[contrato](../../docs/api-contracts/alert-management-v1.yaml) y
[guía de integración](../../docs/architecture/integration-guide.md).
