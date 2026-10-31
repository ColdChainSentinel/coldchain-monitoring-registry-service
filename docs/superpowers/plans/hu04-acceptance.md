# HU-04: primer incremento de aceptación

**Goal:** Ejecutar los tres criterios de HU-04 mediante Gherkin con data tables y generar reportes.
**Architecture:** Suite ubicada en tests/acceptance, compilada por un perfil optativo del servicio Registry. HTTP real en puerto aleatorio, Flyway/JPA/H2 aislado, sin dobles del servicio ni cambios productivos.
**Tech Stack:** Java 17, Spring Boot 3.5.16, Cucumber 7.22.2, JUnit Platform, Maven.
**Spec:** Enunciado 3.6 y criterios de HU-04 del informe, alcance incremental autorizado en el chat.

## Decisiones
- Conservar docs/hu04.feature como única fuente ejecutable; Maven lo copia al classpath.
- Un módulo Maven separado exigiría resolver el JAR ejecutable de Boot como dependencia. El perfil evita cambiar empaquetado y contratos.
- Permanecer en la rama local feat/acceptance-tests y dejar los commits/push al usuario.
- EP03 se incorpora como especificación pendiente, fuera de la selección ejecutable, sin pasos ficticios.
- Este incremento no acredita compatibilidad PostgreSQL, integración de tres servicios ni cumplimiento completo de 3.6.

## Pasos y verificación
- [x] Comprobar rama limpia y baseline: 20 pruebas Registry aprobadas.
- [x] Añadir feature, runner y contexto. Ejecutar y comprobar fallo por pasos sin definir.
- [x] Implementar pasos HTTP: 201 + Location + GET/listado; 409 sin sobrescritura; 400 con errores de campos y sin persistencia.
- [x] Ejecutar suite, pruebas actuales y revisar aserciones HTTP/persistencia.
- [x] Incorporar diseños EP03 pendientes y guía reproducible con alcance y reportes.
- [x] Revisar diff y entregar cambios sin commit ni push.

## Riesgos a revisar
BD de aceptación distinta a los tests existentes; ninguna URL externa ni credenciales; limpieza entre escenarios; comparación de datos y no solo códigos HTTP; ningún pendiente contado como aprobado.

## Registro de ejecución
Baseline: 20/20 Registry. Primera ejecución BDD: tres errores UndefinedStep esperados. Tras implementar pasos: 23/23, sin omitidos y gate JaCoCo aprobado. No se modificó lógica productiva. El workflow remoto aún no se ha ejecutado.

Verificación final: mvnw -B -ntp -Pacceptance clean verify: BUILD SUCCESS. Registry 23, Telemetry 93, Alerts 93: 209 pruebas, sin fallos, errores ni omitidos. Se comprobaron los 15 pasos HU-04 en JSON y la generación del HTML. git diff --check sin errores.
