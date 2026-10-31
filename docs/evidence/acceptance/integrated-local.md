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

## Tercer incremento (30 de octubre de 2026)

Entorno local: OpenJDK 27 compilando con `release 17`, Maven Wrapper 3.9.11 y Docker Engine 29.6.1.
No reemplaza una ejecución con JDK 17 ni el run remoto en Actions.

- `mvnw -B -ntp -Pacceptance clean verify`: BUILD SUCCESS. Registry 28 (20 JUnit + 8
  escenarios HU-04), Telemetry 93 y Alerts 93, sin fallos, errores ni omitidos.
- `mvnw -B -ntp -f tests/integration/pom.xml clean verify` (PostgreSQL): 28 escenarios,
  0 fallos, 0 errores, 0 omitidos; `environment.txt` con `database=postgres`.
- Mismo comando con `-Dintegration.database=h2`: 26 aprobados y 2 omitidos por el filtro
  `not @requiere_postgres`.
- Dry-run de `tests/acceptance/specifications`: 144 escenarios analizados, sin errores
  de sintaxis; no se ejecutan.
- Trivy 0.70.0 local (`fs`, HIGH/CRITICAL, sin hallazgos sin corrección): CVE-2026-54291
  (HIGH) en `org.postgresql:postgresql` 42.7.11 en los tres servicios, corregido en 42.7.12.
  El job `trivy` fallará hasta actualizar la dependencia.

## Remediación DevSecOps (30 de octubre de 2026)

Sobre `develop` actualizado (incluye Terraform y la Function). Trivy 0.70.0 local con el
mismo gate del CI (`--ignore-unfixed`, HIGH/CRITICAL, `.trivyignore.yaml`): código de salida 0.

- pgjdbc 42.7.14: `mvnw -Pacceptance clean verify` BUILD SUCCESS (28/93/93) y suite integrada
  PostgreSQL 28/28; el JAR de Telemetry empaqueta `postgresql-42.7.14.jar`.
- Frontend sin root: imagen construida localmente; `id` dentro del contenedor devuelve
  `uid=101(nginx)` y la SPA responde 200 en `/` y en rutas profundas por el puerto 8080.
- AZU-0012 queda como excepción con vencimiento 2027-03-31.
