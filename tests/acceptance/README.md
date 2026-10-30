# Acceptance Testing — primer incremento HU-04

Suite BDD ejecutable para EP-02 / HU-04, con data tables. Se conservan los tres
criterios de `docs/hu04.feature`, única fuente del feature. Maven copia ese archivo
al classpath; no mantener otra copia editable. Runner y pasos viven en esta carpeta
del monorepo y se compilan mediante el perfil optativo `acceptance` de Registry.

## Requisitos y ejecución

JDK 17 y acceso a Maven Central para la primera descarga. No requiere arrancar
Registry, PostgreSQL, Docker, Telemetry, Alerts ni frontend. Desde la raíz:

```powershell
.\mvnw.cmd -B -ntp -pl services/monitoring-registry -Pacceptance clean verify
```

Linux/macOS: `sh mvnw -B -ntp -pl services/monitoring-registry -Pacceptance clean verify`.
Ejecuta los 20 tests previos y los 3 escenarios Cucumber, además de Checkstyle y gate
JaCoCo. Para solo los escenarios, usar `-Dtest=Hu04AcceptanceTest` con `test` en lugar
de `verify`. Usar `clean` al cambiar entre perfiles para eliminar clases de pruebas
compiladas anteriormente. La compilación normal no añade dependencias Cucumber.

La suite arranca Spring Boot en puerto aleatorio y usa HTTP real contra el servicio.
Controlador, validación, servicio, repositorio y migración Flyway son reales. La base
`jdbc:h2:mem:hu04_acceptance` es distinta a la de los tests previos. Los hooks borran
exclusivamente los puntos de esta base de aceptación antes y después de cada escenario.
La ejecución es secuencial. No usar parámetros que sustituyan su datasource por una
base compartida: el borrado forma parte del aislamiento de pruebas.

## Trazabilidad

| Historia / criterio | Escenario | Comprobaciones |
|---|---|---|
| HU-04 / CA-01 | Registrar punto válido | 201, UUID, Location, datos enviados, GET individual y listado |
| HU-04 / CA-02 | Código duplicado | 409, mensaje, unicidad sin sobrescribir el punto original; incluye mayúsculas/minúsculas |
| HU-04 / CA-03 | Datos ausentes | 400, mensajes para code/name/type y listado vacío |

Las tablas de datos de entrada y de campos obligatorios son DataTables consumidas
por los pasos Java; no se confunden con las tablas `Ejemplos` de Scenario Outline.

## Reportes

Se generan dentro de `services/monitoring-registry/target/`:

- `cucumber/hu04.html`: reporte legible para abrir en el navegador.
- `cucumber/hu04.json` y `cucumber/hu04.xml`: resultados automatizados.
- `surefire-reports/`: suites JUnit y Cucumber.
- `site/jacoco/index.html`: cobertura de Registry, no porcentaje de aceptación.

Los reportes generados quedan ignorados por Git. El workflow `HU-04 Acceptance`
ejecuta el mismo comando en pushes/PR con cambios relevantes y permite ejecución
manual. Publica reportes incluso ante fallo. Su primer resultado remoto debe
comprobarse después de subir la rama: añadir YAML no acredita un run aprobado.

## Diseños de 2.6 todavía pendientes

Se incorporaron a `specifications/ep03/` los tres diseños del informe con etiquetas
`@pendiente @no_implementado`. No se copian al classpath ejecutable ni los selecciona
el runner HU-04. No hay pasos vacíos, mocks que simulen su aprobación ni escenarios
omitidos contados como aceptación completada.

| Historia | Archivo | Impedimento / decisión pendiente |
|---|---|---|
| HU-09 | rangos_temperatura.feature | Faltan API de configuración/consulta y permisos del supervisor. El criterio rechaza min == max; el MVP actual permite igualdad en reglas externas. Resolver antes de automatizar. |
| HU-10 | versiones_rangos.feature | Faltan versiones, autor, vigencias históricas y evaluación según fecha de medición. Se fijaron T0/T1 a fechas UTC concretas para evitar ambigüedad. |
| HU-11 | intervalos_comunicacion.feature | Faltan configuración por dispositivo y detector de falta de comunicación. Al automatizar, usar reloj controlado, sin esperas reales de minutos. |

HU-04 no es la única funcionalidad del repositorio: Telemetry y Alerts ya tienen
código y pruebas. Las reglas externas y sus snapshots no equivalen a implementar
HU-09/HU-10 completas. Estos diseños preservan requisitos pendientes de producto.

## Alcance del apartado 3.6

Este incremento aporta Gherkin automatizado, data tables e integración HTTP/JPA
interna de Registry con H2. No demuestra compatibilidad PostgreSQL ni integración
entre los tres procesos. Para cerrar 3.6 faltan automatizar los demás escenarios
implementables acordados, complementar integración (por ejemplo PostgreSQL con
Testcontainers), verificar CI remoto y analizar sus reportes en el informe.
