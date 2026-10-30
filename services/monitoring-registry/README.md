# ColdChain Sentinel — Monitoring Management

Primer microservicio del proyecto UPC Agile Software Factories, ahora en
`services/monitoring-registry/`. **Empieza por el [README del monorepo](../../README.md)**.
Se conserva el nombre técnico `monitoring-management` y su artefacto original.

## Alcance

HU-04, EP-02: registrar un código único, nombre y tipo de punto; listar registros; rechazar duplicados y campos obligatorios ausentes. Consulta por UUID como complemento técnico. No se implementan aún dispositivos, lecturas, alertas, autenticación ni frontend. Swagger permite demostrar la API. El contenedor solo publica puertos en localhost; esta entrega no debe exponerse como servicio público sin la seguridad del producto final.

## Stack

Java 17, Maven 3.9.11 (wrapper), Spring Boot 3.5.16, Spring Data JPA, PostgreSQL 16, Flyway, Bean Validation, springdoc 2.8.17, JUnit 5, Mockito, JaCoCo y Checkstyle. H2 es una dependencia de test, no forma parte de la aplicación ejecutable.

Este servicio sigue siendo un proyecto Maven independiente, con wrapper y configuración
propios. El POM de la raíz del monorepo es solo un agregador opcional; las rutas del CI
existente se adaptaron al traslado. Ejecuta los comandos siguientes **desde esta carpeta**.
Java 17 permite verificar el mismo código localmente y en GitHub; no es un requisito
del docente usar Java 21.

## Arquitectura

`controller` recibe y valida HTTP; `service` aplica reglas y transacciones; `repository` accede a JPA; `model` representa la tabla; `dto` delimita el contrato; `exception` uniformiza errores. PostgreSQL pertenece a este servicio. Flyway crea el esquema y Hibernate lo valida.

El código se normaliza a mayúsculas con Locale.ROOT y tiene un índice único para proteger también frente a carreras. La validación HTTP ocurre antes de guardar. Se permite código ASCII alfanumérico, guion y guion bajo (máximo 40); nombre no blanco (máximo 120). Estas restricciones de longitud y normalización son decisiones de implementación adicionales a HU-04. Tipos exactos: ALMACENAMIENTO o TRANSPORTE.

| Método | Ruta | Resultado |
|---|---|---|
| POST | /api/v1/monitoring-points | 201 + Location; 400 inválido; 409 duplicado |
| GET | /api/v1/monitoring-points | 200, array ordenado por código |
| GET | /api/v1/monitoring-points/{id} | 200; 400 UUID inválido; 404 inexistente |

## Ejecución sin Docker para la aplicación

Instala JDK 17 y ten PostgreSQL disponible. Define DB_URL, DB_USER y DB_PASSWORD. Para PowerShell:

```powershell
$env:DB_URL='jdbc:postgresql://localhost:5432/coldchain'
$env:DB_USER='coldchain'
$env:DB_PASSWORD='TU-CONTRASENA-LOCAL'
.\mvnw.cmd spring-boot:run
```

Puedes iniciar solo PostgreSQL con `docker compose up -d db` después de crear `.env`. `.env` es leído por Compose, no automáticamente por Maven: exporta las variables al ejecutar Java directamente.

## Verificación

Windows: `.\mvnw.cmd clean verify`. Linux/macOS: `sh mvnw clean verify`.

No necesitas PostgreSQL para tests. Se ejecutan pruebas unitarias Mockito y pruebas HTTP/JPA con H2 en modo PostgreSQL, incluyendo la migración Flyway. H2 no reemplaza la validación final contra PostgreSQL real. Reportes: `target/surefire-reports/`, `target/site/jacoco/index.html`. JaCoCo exige 70% de líneas sobre todo el módulo sin excluir clases; la cobertura combina unitarias e integración HTTP. Checkstyle revisa imports, llaves, nombres y sentencias vacías; no equivale a un análisis de vulnerabilidades.

`mvn verify` genera `target/monitoring-management-0.1.0.jar`. Maven Wrapper descarga Maven al primer uso; requiere Internet. Docker ejecuta igualmente `clean verify` antes de empaquetar.

## GitHub

El workflow corre ante cualquier push, PR hacia main y ejecución manual. Publica reportes incluso si fallan tests, cuando esos archivos existen. Para impedir merges configura la protección de main con el check `Build, lint, tests and coverage` y revisión de PR; el YAML por sí solo no impide merges. La disponibilidad de protección depende de la configuración y plan del repositorio.

## Fuentes técnicas

- https://docs.spring.io/spring-boot/3.5/system-requirements.html
- https://springdoc.org/v2/ (matriz 3.5.x / 2.8.x)
- https://www.jacoco.org/jacoco/trunk/doc/check-mojo.html
- https://docs.github.com/en/actions

Resultados actuales: [verificación](../../docs/architecture/verification.md).
Las evidencias originales permanecen en `../../docs/evidence/` como material histórico.
