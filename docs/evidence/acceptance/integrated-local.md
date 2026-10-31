# Aceptación: segundo incremento (verificación local)

Base: commit `6878ea6`, más los cambios locales de este incremento. No identifica
un commit publicado de la suite integrada: adjuntar el hash tras el commit manual.
Verificación realizada el 30 de octubre de 2026, Java 17.0.12 / Maven Wrapper 3.9.11.

## Backend y HU-04

Comando desde raíz:

```powershell
.\mvnw.cmd -B -ntp -Pacceptance clean verify dependency:build-classpath "-Dmdep.outputFile=target/integration-classpath.txt"
```

BUILD SUCCESS. Pruebas sin fallos, errores ni omitidos:

- alert-management: 93.
- monitoring-registry: 23.
- telemetry-ingestion: 93.

## Integración HTTP real con H2 explícita

```powershell
.\mvnw.cmd -B -ntp -f tests/integration/pom.xml "-Dintegration.database=h2" clean verify
```

BUILD SUCCESS: 12 escenarios, 0 fallos, 0 errores, 0 omitidos. La suite inició
tres JVM reales, con H2 por servicio, y ejecutó HTTP real entre ellas. Casos:

- Almacenar y consultar una medición válida
- Rechazar campos obligatorios ausentes
- Rechazar una medición para un punto inexistente
- Evaluar límites inclusivos y desviaciones
- Evaluar límites inclusivos y desviaciones
- Evaluar límites inclusivos y desviaciones
- Evaluar límites inclusivos y desviaciones
- Evaluar límites inclusivos y desviaciones
- Conservar una medición sin evaluar cuando falta una regla
- Repetir una evaluación sin duplicar mediciones ni alertas
- Rechazar la reutilización del identificador con otros datos
- Conservar la medición pendiente durante una caída de Alerts

Reportes generados (ignorados por Git): `tests/integration/target/cucumber/flow.html`,
`flow.json`, `flow.xml`, `failsafe-reports/` y `stack/environment.txt` (database=h2).
Interpretación: los contratos y el flujo implementado pasan estos escenarios locales;
el modo H2 no valida compatibilidad PostgreSQL ni persistencia de alertas tras reinicio.

## Pendientes que no se cuentan como aprobados

- Ejecución del job remoto `integrated-postgres`: se verificará después del push manual.
- HU-12 completa: dispositivo activo, unidad explícita y fecha de recepción.
- HU-19 completa: dispositivo asociado a la alerta.
- HU-09/HU-10/HU-11: diseños pendientes; no se convierten en casos aprobados mediante dobles.
- Actualización final del apartado 3.6 con hash y enlaces al run CI real.

No se instalaron ni reinstalaron aplicaciones para esta verificación. Maven descargó
bibliotecas de pruebas. Docker conservó sus datos y los respaldos de carpetas
temporales; no se ejecutó reset, prune ni borrado de volúmenes.

## Verificación PostgreSQL después del reinicio

El 30 de octubre de 2026, tras reiniciar Windows, Docker respondió con motor 29.9.2.
Se ejecutó el modo predeterminado, sin solicitar H2:

```powershell
.\mvnw.cmd -B -ntp -f tests/integration/pom.xml clean verify
```

**BUILD SUCCESS: 12 escenarios, 0 fallos, 0 errores y 0 omitidos.** Se comprobó
`database=postgres` en el registro del entorno y todos los pasos del JSON Cucumber
figuran como `passed`. Testcontainers utilizó PostgreSQL 16.6, bases y roles separados,
y los tres servicios se ejecutaron desde sus JAR reales con migraciones Flyway.
El escenario de caída y recuperación de Alerts pasó también en este modo.

El reporte HTML/JSON/XML actual en `tests/integration/target/cucumber/` corresponde
a PostgreSQL. La comprobación previa H2 queda como ejecución histórica distinta,
no como escenarios adicionales. El resultado local PostgreSQL queda validado;
la ejecución en GitHub Actions sigue pendiente del push y del run remoto.

Este arranque correcto no demuestra que el fallo de Docker no pueda reaparecer.
No fue necesario reinstalar Docker para ejecutar esta verificación.
