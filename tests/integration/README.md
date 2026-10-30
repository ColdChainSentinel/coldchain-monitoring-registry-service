# Aceptación integrada del MVP

Suite independiente de pruebas, sin dependencias de clases de negocio ni cambios
a las APIs. Los `.feature` canónicos están en `tests/acceptance/src/test/resources/features/ep04/`
y `ep06/`; este proyecto Maven los copia a su classpath. No forma parte del reactor
normal, para que los desarrolladores no necesiten Docker al compilar servicios.

## Qué verifica

Doce escenarios (contando las cinco filas del esquema de límites):

| Historia / alcance | Casos |
|---|---|
| HU-12 / CA-01 y CA-03, parcial | Medición guardada/consultable, normalización UTC y evaluación completa |
| Contrato de ingesta | Campos ausentes y punto inexistente rechazados |
| HU-19 / CA-01 y CA-02, parcial | Interior, ambos límites, desviación alta y baja; datos y límites en alerta |
| HU-19 / CA-03 | RULE_NOT_CONFIGURED sin inventar alerta |
| Contrato de evaluación | Replay devuelve resultado/alerta originales; otros datos con mismo ID responden 409 |
| Resiliencia | Alerts caído: 201/PENDING; reevaluación externa sin duplicar ingesta; Telemetry permanece PENDING |

**Alcance parcial:** HU-12 aún no valida dispositivo activo, unidad explícita ni fecha
de recepción (CA-02). HU-19 no expone dispositivo en la alerta. Estos tests no acreditan
las historias completas ni las HU-09/HU-10/HU-11. Las reglas externas de prueba no son
una API de configuración térmica. No se reenvía POST de ingesta para recuperar una evaluación.

Los escenarios usan los UUID de las mediciones que crean y comparan conteos iniciales.
No se borran datos por escenario ni se invocan bases existentes del usuario. La suite
es secuencial; sus aserciones no dependen de que otro escenario se ejecute primero.

## Modo PostgreSQL (predeterminado, CI)

Requiere JDK 17, Maven Wrapper, Docker con motor Linux activo y acceso a Maven Central
y al registry de imágenes la primera vez. Desde la raíz:

```powershell
.\mvnw.cmd -B -ntp -Pacceptance clean verify
.\mvnw.cmd -B -ntp -f tests/integration/pom.xml clean verify
```

En Linux/macOS sustituir `.\mvnw.cmd` por `sh mvnw`.

Testcontainers inicia `postgres:16.6-alpine` con puerto dinámico, credenciales
aleatorias y tres bases/roles independientes. El proceso de pruebas solo administra
ese contenedor. Arranca los tres JAR reales en JVM y puertos locales independientes,
sin perfiles smoke/test. Crea puntos mediante Registry y prepara una regla externa
solo para el punto de prueba. Flyway usa las migraciones productivas sin modificarlas.
Comprueba health antes de ejecutar. Al finalizar cierra sus procesos y contenedor.

**Si falta Docker, este modo falla. No omite tests ni cambia silenciosamente a H2.**

## Modo H2 explícito (verificación local sin Docker)

La preparación genera classpaths de test con H2 disponible, sin modificar código
productivo. Se ejecutan las mismas aplicaciones y peticiones HTTP, pero con una
base H2 aislada dentro de cada JVM. No equivale a validar PostgreSQL.

```powershell
.\mvnw.cmd -B -ntp -Pacceptance clean verify dependency:build-classpath "-Dmdep.outputFile=target/integration-classpath.txt"
.\mvnw.cmd -B -ntp -f tests/integration/pom.xml "-Dintegration.database=h2" clean verify
```

Con H2 los datos de Alerts se pierden al reiniciar ese proceso. El escenario de caída
solo exige recuperar la medición nueva de ese escenario. La persistencia de alertas
previas tras reinicios requiere el modo PostgreSQL; este incremento no la acredita.

## Reportes y diagnóstico

En `tests/integration/target/`:

- `cucumber/flow.html`, `flow.json`, `flow.xml`: escenarios, pasos y resultados.
- `failsafe-reports/`: resumen Maven/JUnit; un fallo hace fallar `verify`.
- `stack/environment.txt`: modo de base y puertos utilizados, sin credenciales.
- `stack/registry.log`, `telemetry.log`, `alerts.log`: arranque y llamadas reales.

El modo debe conservarse junto al reporte para interpretar qué se probó. `clean`
elimina reportes anteriores; guardar los que se vayan a usar como evidencia antes de
cambiar de modo. Todo `target/` está ignorado por Git. No se publica `rules.yml` como
configuración del producto.

## CI y pendientes

El workflow `ColdChain Acceptance` conserva el job HU-04 y añade `integrated-postgres`.
Este último verifica y empaqueta los tres servicios, ejecuta PostgreSQL y publica
reportes/logs incluso si falla. Añadir el workflow no acredita un run remoto exitoso:
debe verificarse en Actions después del push manual del usuario.

Tras reiniciar Windows, Docker respondió y se ejecutó el modo PostgreSQL: 12 escenarios
aprobados, sin fallos, errores ni omitidos. También pasaron previamente con H2 explícita.
La validación local PostgreSQL está completada; el run remoto sigue pendiente.
Evidencia y límites: [verificación local](../../docs/evidence/acceptance/integrated-local.md).
No se reinstaló ni restableció Docker. El reinicio permitió esta ejecución; no acredita
una reparación permanente del problema de sockets Windows.
