# Verificación de esta entrega

Ejecución local: 29 de octubre de 2026, Linux/WSL sobre el repositorio de Windows.
Herramientas temporales fuera del repositorio: Temurin **17.0.21**, Maven Wrapper
**3.9.11**, Node **22.22.0**, npm **10.9.4**. No se instaló software globalmente,
ejecutaron despliegues ni modificaron remotos. No se creó ningún commit.

## Resultados ejecutados

| Verificación | Resultado |
|---|---|
| Registry antes del traslado: `sh mvnw -B -ntp verify` | SUCCESS, 20 tests, 0 fallos/errores/omitidos |
| Comparación de archivos trasladados contra `HEAD` | 26 archivos idénticos (código, tests, POM, scripts y Docker); solo README adaptado a ubicación y enlaces actuales |
| Raíz después del traslado: `sh mvnw -B -ntp clean verify` | SUCCESS en los tres servicios y agregador |
| Registry después del traslado | 20 tests, 0 fallos/errores/omitidos; Checkstyle aprobado |
| JaCoCo Registry | 49/51 líneas, **96,08 %**, gate 70 % aprobado |
| Telemetry | 3 tests HTTP, 0 fallos/errores/omitidos |
| Alerts | 3 tests HTTP, 0 fallos/errores/omitidos |
| Inspección de los tres JAR | Sin H2 ni perfiles de configuración de test empaquetados |
| Frontend: `npm ci` | Instalación desde lockfile correcta |
| Frontend: `npm run typecheck` | Aprobado |
| Frontend: `npm test` | 3 tests del cliente HTTP aprobados |
| Frontend: `npm run build` | Build Vite aprobado, salida `frontend/dist/` |
| Frontend: `npm audit` | **0 vulnerabilidades reportadas** en el árbol instalado |
| Los dos YAML con `@apidevtools/swagger-parser@12.1.0` | OpenAPI 3.0.3 válido, versión de contrato 0.1.0 |
| `git diff --check` | Sin errores |
| Script de resumen del CI en nueva carpeta de Registry | Lee reportes y cobertura correctamente |

Total automatizado: **26 pruebas Java + 3 frontend = 29**, sin fallos.
Los nuevos tests arrancan Tomcat en puertos aleatorios y consultan salud, liveness,
readiness (con DB), OpenAPI, Swagger y ausencia de rutas de negocio. Registry
conserva sus pruebas Mockito y HTTP/JPA/Flyway con H2.

Versiones resueltas de frontend en el lockfile: React 19.3.1, Vite 7.3.7,
TypeScript 5.9.3 y Vitest 5.0.5. El lockfile, no un rango sin resolver, es la base
de `npm ci`.

## Incidencias encontradas y resueltas

- El entorno inicialmente no tenía Java/Node Linux en PATH. Se descargaron herramientas
  a `/tmp/opencode/coldchain-tools`, sin modificar el sistema ni el repositorio.
- El primer intento de baseline superó 120 segundos; se repitió sin ese límite y
  terminó correctamente. No se presenta el intento interrumpido como éxito.
- La primera selección de Vitest 3 reportó tres vulnerabilidades transitivas. Se cambió
  a Vitest 5.0.5 compatible con Node 22.12+ y Vite 7, se regeneró lockfile y se repitieron
  instalación limpia, pruebas, typecheck, build y auditoría. La selección intermedia
  de Vitest 4 encontró un fallo de resolución de peers de npm (`edgesOut`); no forma
  parte del estado final verificado. No se usó `npm audit fix --force`.

## Arranque conjunto comprobado

Se levantaron simultáneamente los tres servicios con `spring-boot:test-run` (Registry
con perfil `test`, los nuevos con `smoke`) y `npm run dev`. Comprobación HTTP real:

- `http://127.0.0.1:8080/actuator/health`: **200 UP**.
- `http://127.0.0.1:8081/actuator/health`: **200 UP**.
- `http://127.0.0.1:8082/actuator/health`: **200 UP**.
- `http://127.0.0.1:5173/`: **200**, HTML de ColdChain Sentinel.
- Los tres prefijos Vite `/registry`, `/telemetry`, `/alerts` reenviaron
  `/actuator/health` correctamente: **200 UP** en los tres.
- Vite sirvió `/src/main.tsx` transformado: **200**.
- `/v3/api-docs` en los tres puertos Java: **200**, JSON OpenAPI válido.

Se cerraron únicamente los procesos iniciados para esta comprobación. No quedan
servicios ejecutándose como parte de la entrega. Esta prueba usa **H2**, no PostgreSQL,
y no ejecuta un navegador ni operaciones funcionales de mediciones/alertas.

Para repetir manualmente, abrir cuatro terminales, ejecutar desde cada servicio:

```sh
# Registry:
sh mvnw spring-boot:test-run -Dspring-boot.run.profiles=test
# Telemetry y Alerts (cada uno en su propia carpeta/terminal):
sh mvnw spring-boot:test-run -Dspring-boot.run.profiles=smoke
# Desde frontend, cuarta terminal:
npm run dev
```

En otra terminal consultar con `curl -f http://localhost:8081/actuator/health` y
`curl -f http://localhost:5173/telemetry/actuator/health`, análogamente para los otros
puertos/prefijos. En PowerShell usar `.\mvnw.cmd` y `curl.exe`.

## Límites de la evidencia

- Bases de datos de pruebas: **H2**, no PostgreSQL real. No se verificó despliegue ni
  arranque del JAR con una base PostgreSQL externa; los JAR se compilaron y empaquetaron.
- Contratos nuevos validados estructuralmente, no con implementación de negocio.
  Persistencia de mediciones, evaluación, idempotencia y reglas siguen pendientes.
- No se ejecutó GitHub Actions remotamente; solo se adaptaron rutas del workflow
  existente y se comprobaron localmente su build, checks y script de reportes.
- No se ejecutaron Docker/Compose, Terraform, Kubernetes, Azure ni pruebas E2E de
  navegador. No existe suite transversal de aceptación todavía.
- Auditoría npm es una fotografía del árbol y avisos actuales, no una certificación
  de seguridad ni análisis de dependencias Java.
- `docs/evidence/` es material histórico del proyecto original, no evidencia nueva.
