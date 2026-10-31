# Segundo incremento: aceptación integrada

**Goal:** Automatizar el flujo real de mediciones y alertas del MVP y complementar 3.6 con integración PostgreSQL en CI.
**Architecture:** Proyecto de tests independiente en tests/integration, sin dependencia de clases de negocio. Tres JVM reales, HTTP real y bases separadas. Gherkin centralizado en tests/acceptance. PostgreSQL/Testcontainers por defecto; modo H2 explícito para verificación local cuando Docker no inicia.
**Tech Stack:** Java 17, Cucumber 7.22.2, JUnit 5, Testcontainers 2.0.4, Maven Failsafe.
**Spec:** Enunciado 3.6, HU-12/HU-19 del informe y contratos del MVP. Alcance autorizado: continuar el siguiente incremento sin commits automáticos.

## Decisiones
- HU-12/HU-19 se marcan como alcance parcial: faltan dispositivo activo, recepción y dispositivo en alerta. No se implementa producto nuevo para hacer pasar escenarios.
- Suite externa evita tocar los pom y fuentes productivos; no depende del CI de otro integrante.
- Por defecto falla si Docker no está disponible; no se omite PostgreSQL silenciosamente. H2 se solicita explícitamente y no se cuenta como prueba PostgreSQL.
- Escenarios aislados por UUID de medición y conteos iniciales; no se borran registros ni bases del usuario.
- No se modifica EP03 ni se afirma que su diseño esté implementado.
- GitHub ejecuta PostgreSQL; el resultado remoto seguirá pendiente hasta que el usuario suba la rama.

## Pasos
- [x] Confirmar commit 6878ea6 y rama limpia.
- [x] Investigar Docker: fallos recurrentes de sockets incluso tras respaldo de carpetas temporales. No resetear ni reinstalar.
- [x] Crear 12 escenarios y runner. Verificar fallo por pasos ausentes.
- [x] Implementar entorno y pasos reales; verificar localmente con H2 explícito.
- [x] Añadir job PostgreSQL y documentación. Ambos modos verificados localmente: 12 escenarios en H2 y 12 en PostgreSQL; CI remoto pendiente.
- [x] Revisar resultados, alcance parcial y estado Git sin commit/push.

## Resultados locales
- Primera ejecución: 12 escenarios con UndefinedStep, sin pasos simulados.
- Reactor con perfil acceptance: 209 pruebas aprobadas (23 Registry, 93 Telemetry, 93 Alerts).
- Suite integrada explícita H2: 12 pruebas aprobadas, 0 fallos/errores/omitidos; tres procesos reales y caída/recuperación de Alerts.
- PostgreSQL/Testcontainers: tras reiniciar Windows, 12/12 escenarios aprobados, sin fallos, errores ni omitidos. Entorno confirmado database=postgres.
- Se preservó el reporte H2 antes de ejecutar PostgreSQL; el reporte actual corresponde a PostgreSQL.
- Preferencia del usuario: nombres de planes sin fecha. Los commits y push quedan a su cargo.

Verificación después del reinicio: Docker Engine 29.9.2 disponible; mvnw -f tests/integration/pom.xml clean verify termina BUILD SUCCESS. No se realizó commit ni push.
