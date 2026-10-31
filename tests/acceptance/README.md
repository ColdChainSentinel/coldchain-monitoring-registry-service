# Acceptance Testing

Matriz completa de las 36 HU al final de esta guía.

## Primer incremento: HU-04

Suite BDD ejecutable para EP-02 / HU-04, con data tables. Los ocho escenarios cubren los
tres criterios de `docs/hu04.feature`, única fuente del feature. Maven copia ese archivo
al classpath; no mantener otra copia editable. Runner y pasos viven en esta carpeta
del monorepo y se compilan mediante el perfil optativo `acceptance` de Registry.

## Requisitos y ejecución

JDK 17 y acceso a Maven Central para la primera descarga. No requiere arrancar
Registry, PostgreSQL, Docker, Telemetry, Alerts ni frontend. Desde la raíz:

```powershell
.\mvnw.cmd -B -ntp -pl services/monitoring-registry -Pacceptance clean verify
```

Linux/macOS: `sh mvnw -B -ntp -pl services/monitoring-registry -Pacceptance clean verify`.
Ejecuta los 20 tests previos y los 8 escenarios Cucumber, además de Checkstyle y gate
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
| HU-04 / CA-01 | Un punto por tipo (ALMACENAMIENTO, TRANSPORTE) | 201 y consulta para ambos tipos admitidos |
| HU-04 / CA-01 | Tipo no admitido (contrato) | 400 con los tipos válidos, sin guardar |
| HU-04 / CA-02 | Código duplicado | 409, mensaje, unicidad sin sobrescribir el punto original; incluye mayúsculas/minúsculas |
| HU-04 / CA-03 | Datos ausentes | 400, mensajes para code/name/type y listado vacío |
| HU-04 / CA-03 | Falta code o name | 400 identifica el campo y no guarda |

Las tablas de datos de entrada y de campos obligatorios son DataTables consumidas
por los pasos Java; no se confunden con las tablas `Ejemplos` de Scenario Outline.

## Reportes

Se generan dentro de `services/monitoring-registry/target/`:

- `cucumber/hu04.html`: reporte legible para abrir en el navegador.
- `cucumber/hu04.json` y `cucumber/hu04.xml`: resultados automatizados.
- `surefire-reports/`: suites JUnit y Cucumber.
- `site/jacoco/index.html`: cobertura de Registry, no porcentaje de aceptación.

Los reportes generados quedan ignorados por Git. El workflow `ColdChain Acceptance`
ejecuta el mismo comando en pushes/PR con cambios relevantes y permite ejecución
manual. Publica reportes incluso ante fallo. Su primer resultado remoto debe
comprobarse después de subir la rama: añadir YAML no acredita un run aprobado.

## Diseños de 2.6 todavía pendientes

Se incorporaron a `specifications/ep03/` los tres diseños del informe con etiquetas
`@pendiente @no_implementado`. No se copian al classpath ejecutable ni los selecciona
ningún runner. No hay pasos vacíos, mocks que simulen su aprobación ni escenarios
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
interna de Registry con H2. Por sí solo no demuestra compatibilidad PostgreSQL ni integración
entre los tres procesos; consultar el segundo incremento al final de esta guía. Para cerrar 3.6 faltan automatizar los demás escenarios
implementables acordados, complementar integración (por ejemplo PostgreSQL con
Testcontainers), verificar CI remoto y analizar sus reportes en el informe.

## Segundo incremento: mediciones y alertas integradas

Se añaden los features EP04/HU12 y EP06/HU19 con alcance parcial y casos de contrato.
Los ejecuta el proyecto independiente [tests/integration](../integration/README.md),
no el runner HU-04. Hay modo PostgreSQL/Testcontainers por defecto y H2 explícito.
La ejecución de HU-04 conserva sus tres escenarios y su base aislada.
Los diseños EP03 continúan pendientes. Consultar la guía de integración para los
comandos, resultados por modo y faltantes funcionales.

## Tercer incremento: cobertura de todas las HU

Se ampliaron los escenarios ejecutables donde el MVP ya tiene comportamiento real y se
escribieron especificaciones pendientes para el resto de criterios. Regla aplicada: un
escenario solo es ejecutable si prueba comportamiento existente; nunca se aserta un
comportamiento actual que contradiga un CA (por ejemplo, una alerta por cada medición
fuera de rango contradice HU-20/CA-01).

- Ejecutables nuevos (suite integrada): `features/ep04/calidad_mediciones.feature`,
  `historial_mediciones.feature`, `resiliencia_registry.feature`,
  `features/ep06/contrato_evaluacion.feature` y `limites_originales.feature`.
- Pendientes nuevos: `specifications/ep01` a `ep09`, un archivo por HU (o
  `*_pendiente.feature` con solo los CA que faltan en HU con alcance parcial).
  Todos llevan `@pendiente @no_implementado` y no tienen step definitions.
- Validación de sintaxis de los pendientes (sin ejecutarlos):
  `java -cp <classpath de tests/integration> io.cucumber.core.cli.Main --dry-run --glue none tests/acceptance/specifications`.
  Resultado local: 144 escenarios analizados, todos `undefined`, sin errores de parseo.

En Gherkin `# language: es`, Cucumber interpreta `{bigdecimal}` con coma decimal: escribir
`7,5` en el texto de un paso. Las tablas de datos se leen con `new BigDecimal` y usan punto.

### Matriz de trazabilidad

Leyenda: **E** ejecutable y aprobado; **P** parcial (ejecuta lo implementado, el resto
en pendiente); **D** solo diseño pendiente.

| HU | Estado | Ejecutable | Pendiente |
|---|---|---|---|
| HU-01 a HU-03 | D | — | `specifications/ep01/` |
| HU-04 | E | `docs/hu04.feature` (CA-01..03) | — |
| HU-05 a HU-08 | D | — | `specifications/ep02/` |
| HU-09 | D | Límites inclusivos usados por reglas externas en `ep06/desviaciones.feature` | `ep03/rangos_temperatura.feature` |
| HU-10 | P | CA-03 (snapshot de límites) en `ep06/limites_originales.feature`, solo PostgreSQL | `ep03/versiones_rangos.feature` |
| HU-11 | D | — | `ep03/intervalos_comunicacion.feature` |
| HU-12 | P | CA-01 sin dispositivo/unidad, CA-03 (`ep04/mediciones.feature`) | CA-01 completo, CA-02 (`ep04/hu12_recepcion_pendiente.feature`) |
| HU-13 | P | CA-01 temperatura/fecha, CA-03 sin alertas (`ep04/calidad_mediciones.feature`) | dispositivo, unidad, motivo registrado (`ep04/hu13_calidad_pendiente.feature`) |
| HU-14 | P | CA-01/CA-03 en Alerts, CA-02 historial (`ep06/desviaciones.feature`, `ep04/historial_mediciones.feature`) | Telemetry por ID de medición, dashboard (`ep04/hu14_duplicados_pendiente.feature`) |
| HU-15 | D | — | `ep04/hu15_comunicacion.feature` |
| HU-16, HU-17 | D | — | `specifications/ep05/` |
| HU-18 | P | CA-01 por punto ordenado, CA-03 vacío (`ep04/historial_mediciones.feature`) | dispositivo, período, columnas (`ep05/hu18_historial_pendiente.feature`) |
| HU-19 | P | CA-01, CA-03 y CA-02 sin dispositivo (`ep06/desviaciones.feature`, `ep06/contrato_evaluacion.feature`) | dispositivo en la alerta (`ep06/hu19_desviaciones_pendiente.feature`) |
| HU-20 a HU-24 | D | — | `specifications/ep06/` |
| HU-25 a HU-27 | D | — | `specifications/ep07/` |
| HU-28 a HU-34 | D | — | `specifications/ep08/` |
| HU-35, HU-36 | D | — | `specifications/ep09/` |

### Observaciones para el backlog

- HU-20/CA-01: el MVP crea una alerta por medición fuera de rango; no agrupa eventos.
- HU-23/CA-02: Alerts lista en orden ascendente de creación; el CA pide el inverso.
- HU-09/CA-02: las reglas externas admiten mínimo igual a máximo; el CA lo rechaza.
- HU-14/CA-01: Telemetry no recibe un identificador de medición propio; reenviar crea otra.
