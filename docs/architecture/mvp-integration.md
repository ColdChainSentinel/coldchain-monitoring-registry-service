# ColdChain Sentinel — integración funcional del MVP

## Alcance y entorno

Verificación local del **30 de octubre de 2026**, Linux/WSL sobre el monorepo existente.
No se agregan funcionalidades, endpoints, contratos, dependencias productivas ni
infraestructura. Los cambios de las fases anteriores permanecen sin commit y se preservan.

El reinicio del entorno interrumpió los primeros comandos y eliminó `/tmp/opencode`,
incluido el clúster temporal previo. Esos intentos **no se cuentan como pruebas aprobadas**.
No había un PostgreSQL del equipo accesible con credenciales confirmadas: se restauraron
herramientas temporales y se creó un clúster aislado PostgreSQL **16.6**, limitado a
loopback, puerto 55441. Se crearon tres bases exclusivas para suites y otras tres para
la demostración; no se abrieron, borraron ni migraron bases del usuario/equipo.

Herramientas: Java **17.0.21**, Maven Wrapper **3.9.11**, Node **22.22.0**, npm **10.9.4**,
Vite **7.3.7**, Vitest **5.0.5** y Chromium **156.0.8078.4**. Los JAR se ejecutan sin
perfiles de test: H2 solo se usa en la primera ejecución de suites, no en el flujo real.
La autenticación trust del clúster desechable es solo de verificación local, no una
configuración recomendada para bases compartidas ni producción.

## Arquitectura integrada

```text
Navegador React
  │ mismo origen: Vite 5173 (preview local del bundle: 4173)
  ├─ /registry/*  ──proxy──> Registry 8080 ──> PostgreSQL: base Registry
  ├─ /telemetry/* ──proxy──> Telemetry 8081 ─> PostgreSQL: base Telemetry
  └─ /alerts/*    ──proxy──> Alerts 8082 ────> PostgreSQL: base Alerts

Telemetry ──GET punto UUID──> Registry
Telemetry ──POST medición confirmada──> Alerts ──GET punto UUID──> Registry
                                      │
                                      └─ regla externa por UUID
                                         + evaluación/alerta local atómica
```

Registry es dueño de `monitoring_points`; Telemetry de `measurements`; Alerts de
`measurement_evaluations` y `alerts`. No hay consultas SQL ni claves foráneas entre
bases, librería de dominio compartida, broker, discovery o gateway.
Flyway aplica las migraciones propias y Hibernate usa `validate`, no `update`.

Telemetry confirma primero la medición PENDING, llama a Alerts fuera de la transacción
y actualiza el resultado en otra transacción. Alerts guarda también las evaluaciones
NO_ALERT. `measurementId` es su clave de idempotencia; solicitudes concurrentes con el
mismo contenido devuelven el ganador y con contenido distinto responden 409.

## Configuración necesaria

| Componente | Puerto local | Variables por proceso |
|---|---:|---|
| Registry | 8080 | `DB_URL` (default `jdbc:postgresql://localhost:5432/coldchain`), `DB_USER` (default heredado `coldchain`), `DB_PASSWORD` requerida, `PORT` opcional |
| Telemetry | 8081 | `DB_URL` (default `jdbc:postgresql://localhost:5432/coldchain_telemetry`), `DB_USER` y `DB_PASSWORD` requeridas, `REGISTRY_BASE_URL`, `ALERTS_BASE_URL`, `PORT` opcional |
| Alerts | 8082 | `DB_URL` (default `jdbc:postgresql://localhost:5432/coldchain_alerts`), `DB_USER` y `DB_PASSWORD` requeridas, `REGISTRY_BASE_URL`, `SPRING_CONFIG_ADDITIONAL_LOCATION`, `PORT` opcional |
| React/Vite | 5173 | `VITE_REGISTRY_BASE_URL=/registry`, `VITE_TELEMETRY_BASE_URL=/telemetry`, `VITE_ALERTS_BASE_URL=/alerts` |

URLs HTTP por defecto: Registry `http://localhost:8080`, Alerts `http://localhost:8082`.
Los dos clientes backend admiten `HTTP_CONNECT_TIMEOUT=2s` y `HTTP_READ_TIMEOUT=5s`.
En la demostración se usó lectura de 3s; el timeout frontend existente es 20s.
No se cambiaron defaults del código. Las variables `VITE_*` son públicas y se fijan al
compilar: nunca colocar credenciales en ellas. Maven/Java **no cargan `.env` automáticamente**.

### CORS y URLs

No hay configuración `@CrossOrigin` ni CORS permisiva en los servicios. El navegador
usa rutas relativas del mismo origen; Vite elimina el prefijo y reenvía cada solicitud.
Esto permite el desarrollo local sin modificar CORS del backend.

Las URLs absolutas a puertos backend desde el navegador constituyen otro origen y
requieren una política CORS acordada o proxy de despliegue. No se introduce `*` para
ocultar ese problema. El servidor Vite/preview es solo de comprobación local, no hosting
productivo. Ver resultados ejecutados de proxy y preflight más adelante.

## Endpoints y contratos

| Servicio | Endpoints existentes utilizados |
|---|---|
| Registry | POST/GET `/api/v1/monitoring-points`; GET `/api/v1/monitoring-points/{id}` |
| Telemetry | POST `/api/measurements`; GET `/api/measurements?monitoringPointId={uuid}` |
| Alerts | POST `/api/alerts/evaluate`; GET `/api/alerts?monitoringPointId={uuid}` |
| Los tres | GET `/actuator/health`, `/v3/api-docs`, Swagger `/swagger-ui.html` |

Registry publica su contrato vigente en `/v3/api-docs`. Los contratos canónicos de
[Telemetry 0.1.1](../api-contracts/telemetry-ingestion-v1.yaml) y
[Alerts 0.2.0](../api-contracts/alert-management-v1.yaml) permanecen sin cambios.
Los servicios generan además OpenAPI en ejecución. UUID, números JSON Celsius y fechas
RFC 3339/UTC se mantienen compatibles. `createdAt` es creación de alerta, no fecha de medición.

React registra puntos y mediciones y consulta alertas; **no evalúa mediciones para leer
estados** ni modifica reglas. El POST de evaluación lo usa Telemetry; el diagnóstico
técnico puede repetir evaluaciones existentes de forma idempotente.

## Reglas externas, sin límites universales

Registry solo contiene UUID, código, nombre y tipo; no dispone de campos térmicos.
Alerts carga reglas explícitas por UUID desde un archivo externo. Ambos umbrales son
obligatorios, deben caber en NUMERIC y cumplir min <= max; una regla inválida impide
el arranque. Sin regla para un punto existente: 422 `THERMAL_RULE_NOT_CONFIGURED`.

Ejemplo **solo para un punto de prueba**, sustituyendo `<UUID_REAL>` por el registrado:

```yaml
coldchain:
  alerts:
    rules:
      "<UUID_REAL>":
        threshold-min: 2
        threshold-max: 8
```

El archivo no es configuración predeterminada ni debe reutilizarse para productos
reales sin aprobación. En esta verificación se escribió fuera del repositorio, después
de obtener un UUID real desde React. Se reinició solo Alerts para cargarlo.

## Cómo ejecutar y reproducir localmente

### 1. Compilar y probar sin infraestructura nueva

Desde la raíz, con JDK 17:

```powershell
.\mvnw.cmd clean verify
```

Desde `frontend/`, con Node 22.22+ recomendado:

```sh
npm ci
npm run typecheck
npm test
npm run build
```

En Linux/macOS usar `sh mvnw` en lugar de `.\mvnw.cmd`. El agregador usa H2 por defecto.
Para suites PostgreSQL utilizar bases **exclusivas y desechables** por servicio:
sus pruebas borran datos. No ejecutar el agregador con un único TEST_DB_URL compartido.

### 2. Arrancar los tres procesos con bases independientes

Usar las bases locales ya aprobadas por el equipo y un usuario por servicio. No se
requiere recrearlas si existen; no ejecutar DROP, repair ni clean de Flyway.
Ejemplo PowerShell, cada bloque desde una terminal nueva y su carpeta de servicio:

```powershell
# services/monitoring-registry/
$env:DB_URL='jdbc:postgresql://localhost:5432/coldchain'
$env:DB_USER='USUARIO_LOCAL_REGISTRY'
$env:DB_PASSWORD=Read-Host 'Contraseña PostgreSQL de Registry'
$env:PORT='8080'
java -jar target/monitoring-management-0.1.0.jar
```

```powershell
# services/telemetry-ingestion/
$env:DB_URL='jdbc:postgresql://localhost:5432/coldchain_telemetry'
$env:DB_USER='USUARIO_LOCAL_TELEMETRY'
$env:DB_PASSWORD=Read-Host 'Contraseña PostgreSQL de Telemetry'
$env:PORT='8081'
$env:REGISTRY_BASE_URL='http://localhost:8080'
$env:ALERTS_BASE_URL='http://localhost:8082'
java -jar target/telemetry-ingestion-0.1.0.jar
```

```powershell
# services/alert-management/
$env:DB_URL='jdbc:postgresql://localhost:5432/coldchain_alerts'
$env:DB_USER='USUARIO_LOCAL_ALERTS'
$env:DB_PASSWORD=Read-Host 'Contraseña PostgreSQL de Alerts'
$env:PORT='8082'
$env:REGISTRY_BASE_URL='http://localhost:8080'
# Primera ejecución puede omitir el archivo: no habrá reglas predeterminadas.
java -jar target/alert-management-0.1.0.jar
```

Verificar `http://localhost:8080/actuator/health`, 8081 y 8082: deben responder 200 UP.
Conservar credenciales fuera de Git. No usar el perfil `smoke` para esta demostración
PostgreSQL: es un perfil exclusivamente de tests.

### 3. Registrar el punto desde React y cargar su regla

En cuarta terminal, desde `frontend/`, `npm run dev`. Abrir http://localhost:5173:

1. **Puntos** → registrar código único, nombre y tipo. Copiar el UUID real de la lista.
2. Crear el YAML externo con ese UUID y límites de prueba aprobados.
3. Detener únicamente Alerts con Ctrl+C. En su misma terminal definir:

```powershell
$env:SPRING_CONFIG_ADDITIONAL_LOCATION='file:C:/ruta/externa/thermal-rules.yml'
java -jar target/alert-management-0.1.0.jar
```

En Linux usar `file:/ruta/absoluta/thermal-rules.yml`. La configuración se carga al
arrancar; modificar el archivo no promete recarga dinámica.

### 4. Demostrar y consultar

En **Mediciones**, seleccionar el punto y registrar 5, 9, 1, 2 y 8 °C, con fechas
válidas y la regla ilustrativa 2–8. Todos se guardan; solo 9 y 1 generan HIGH/LOW.
En **Alertas** verificar sus UUID de medición, límites snapshot y estado OPEN.
El resumen cuenta datos del punto seleccionado, no inventa totales de otros puntos.

Para comprobar persistencia, guardar los UUID, detener los tres procesos, arrancarlos
con **las mismas bases y archivo de reglas** y volver a consultar desde React. No
recrear bases ni volver a enviar las mediciones. Un reinicio no debe generar registros nuevos.

### 5. Fallos controlados, solo en entorno de pruebas

Detener Alerts e introducir otra medición: Telemetry debe confirmar 201 PENDING.
Actualizar la UI debe mostrar la asociación de alertas como no confirmada, nunca cero
por defecto ni NO_ALERT. Arrancar Alerts y repetir **solo POST alerts/evaluate** con
measurementId, punto, temperatura y timestamp originales recupera la evaluación sin
duplicar ingesta. POST measurements no es idempotente y no se debe reenviar por este fallo.

## Semántica de evaluación y límites conocidos

- COMPLETED indica evaluación completada, con o sin alerta.
- NO_ALERT es un resultado de Alerts; **no aparece en GET measurements**. La UI
  solo informa ausencia de alerta registrada cuando pudo consultar Alerts y Telemetry
  indica COMPLETED. No deduce NO_ALERT de PENDING ni de un error de consulta.
- Un reintento externo puede crear una alerta y dejar PENDING en Telemetry. Ambos hechos
  se muestran por separado; no hay reconciliación automática.
- Una evaluación ya guardada se devuelve aunque Registry esté caído o las reglas cambien.
  Una evaluación nueva requiere Registry y una regla explícita.
- Las llamadas a red ocurren fuera de las transacciones locales. No existe transacción
  distribuida, cola durable, reintento automático, scheduler ni garantía exactly-once.

## Resultados de ejecución

### Suites y compilación realmente ejecutadas

| Componente | H2, `clean verify` raíz | PostgreSQL 16.6, `verify` por proyecto |
|---|---:|---:|
| Registry | 20 pruebas, 0 fallos/errores/omitidos | 20 pruebas, 0 fallos/errores/omitidos |
| Telemetry | 93 pruebas, 0 fallos/errores/omitidos | 93 pruebas, 0 fallos/errores/omitidos |
| Alerts | 93 pruebas, 0 fallos/errores/omitidos | 93 pruebas, 0 fallos/errores/omitidos |
| **Total backend** | **206** | **206, repetición de los mismos casos** |

El reactor Maven terminó en BUILD SUCCESS y empaquetó los tres JAR. Registry pasó
Checkstyle y JaCoCo: **49/51 líneas, 96,08 %**, superior al gate heredado del 70 %.
Cada suite PostgreSQL recibió una base de pruebas distinta; no se reemplazaron las
migraciones ni los perfiles del código. Ejemplo del comando ejecutado por proyecto:

```sh
sh mvnw -B -ntp \
  -Dspring.datasource.url=jdbc:postgresql://127.0.0.1:55441/BASE_DE_PRUEBA_EXCLUSIVA \
  -Dspring.datasource.username=USUARIO_DE_PRUEBA \
  -Dspring.datasource.password= verify
```

La contraseña vacía corresponde únicamente al clúster aislado trust de este ensayo,
no a una recomendación para el entorno del equipo. Nunca usar estas suites sobre
bases compartidas/productivas: contienen limpieza de registros de prueba.

Frontend: instalación limpia `npm ci`, `npm run typecheck`, **63 pruebas en 10 archivos**,
`npm run build` y `npm audit` satisfactorios; auditoría con **0 vulnerabilidades
reportadas**. No hay certificación de seguridad implícita en ese resultado.

### Flujo React → APIs → PostgreSQL comprobado

Se usaron JAR reales y navegador Chromium sin mocks ni interceptación de solicitudes.
Vite sirvió React en 5173. El punto se registró desde su formulario y obtuvo el UUID
real **`5a8f7c20-6dc5-4ebd-acaa-9d2fb447c38f`**, código `MVP-INTEGRATION`.
Los UUID de esta sección pertenecen solo a las bases desechables de este ensayo.

1. **Estados vacíos y validación:** catálogo inicialmente vacío; formulario incompleto
   no emitió POST. El registro válido obtuvo 201, seleccionó el UUID y actualizó la
   lista; repetir código devolvió 409 visible. Los historiales inicialmente eran `[]`.
2. **Sin regla:** con Alerts inicialmente sin reglas, una medición de 6 °C se guardó
   como 201 RULE_NOT_CONFIGURED, sin alerta. La comprobación directa del endpoint de
   evaluación devolvió 422 con el código contractual.
3. **Regla explícita:** se escribió el archivo externo 2–8 para ese único UUID y se
   reinició Alerts. No se modificó Registry ni se incluyeron límites en Git como configuración.
4. **Casos funcionales registrados desde React:**

| Temperatura | UUID de medición | Respuesta Telemetry | Resultado original de Alerts |
|---:|---|---|---|
| 5 °C | `040a547e-7916-499c-a94a-feb5d01cec27` | 201 COMPLETED | NO_ALERT |
| 9 °C | `59ee4f81-871a-4986-9a30-7dd8acc52d2d` | 201 COMPLETED | ALERT_CREATED / HIGH_TEMPERATURE |
| 1 °C | `6cd0cb4e-7335-47a6-ab8f-5250a4524236` | 201 COMPLETED | ALERT_CREATED / LOW_TEMPERATURE |
| 2 °C | `5c9f6f04-458d-4c40-9588-c63fa8ee9ac5` | 201 COMPLETED | NO_ALERT (límite inferior inclusivo) |
| 8 °C | `22c129b5-73cb-40b3-a3b3-d4cf04026f96` | 201 COMPLETED | NO_ALERT (límite superior inclusivo) |

La consulta desde React mostró **1 punto, 6 mediciones y 2 alertas**; las seis incluyen
el caso previo sin regla. Las alertas tenían OPEN, measurementId correcto y snapshots
2–8. El NO_ALERT se confirmó repitiendo idempotentemente la evaluación ya almacenada
desde el diagnóstico técnico, no agregando un endpoint ni una mutación a la interfaz.

5. **Persistencia:** una inspección JDBC confirmó una base Registry con un punto,
   otra Telemetry con seis mediciones y otra Alerts con dos alertas y cinco evaluaciones.
   Se detuvieron los tres JAR **y PostgreSQL** y se arrancaron sobre los mismos archivos
   y bases. Las respuestas de puntos, mediciones y alertas resultaron **idénticas** en
   UUID, valores, fechas y estados. Un nuevo navegador consultó ambos historiales desde React.
6. **Indisponibilidad de Alerts:** se detuvo Alerts; React registró 30 °C una única vez.
   Telemetry respondió 201 PENDING (`87c89cda-d434-4b23-b59a-50b86f655ad2`), y el contador
   pasó a siete mediciones. La UI avisó que PENDING no es NO_ALERT y dejó desconocido
   el número de alertas, sin sustituir el fallo por cero.
7. **Recuperación sin duplicar ingesta:** al arrancar Alerts se repitió solo su evaluación,
   generando una tercera alerta. Repetirla devolvió exactamente el mismo resultado.
   Telemetry conservó siete mediciones y el PENDING original, según su limitación documentada.
8. **Bundle compilado:** Vite preview en 4173 sirvió el JS de `dist/assets/`; React
   consultó los datos reales mediante el proxy heredado: un punto, siete mediciones
   y tres alertas. Es verificación local de build, **no** una validación de hosting productivo.
9. **Indisponibilidad de Registry:** la UI mostró error de catálogo, no un falso estado
   vacío. Una evaluación existente pudo repetirse (200); nuevos POST de medición y
   evaluación respondieron 503. No hubo errores JavaScript no capturados en ninguna fase.

### HTTP, contratos y CORS comprobados

- Documentos válidos mediante swagger-parser 12.1.0: Registry generado OpenAPI 3.1.0
  (`info.version: v0`, metadato heredado), Telemetry canónico 3.0.3/0.1.1 y generado
  3.1.0/0.1.1, Alerts canónico 3.0.3/0.2.0 y generado 3.1.0/0.2.0. No se cambió ningún contrato.
- **77 respuestas reales** de las APIs y del recorrido React se validaron con AJV 8
  y formatos UUID/date-time contra sus esquemas. Se validaron también 21 cuerpos de
  solicitud compatibles y se identificaron tres cuerpos deliberadamente inválidos.
  Es validación de los mensajes capturados, no prueba exhaustiva de todos los esquemas.
- Se comprobaron 200/201, 400 por entrada inválida, 404 de punto inexistente, 409 de
  conflicto, 422 por regla ausente y 503 de dependencia no disponible.
- Los tres preflight directos, con Origin `http://localhost:5173`, método POST y
  content-type JSON, respondieron **403 sin Access-Control-Allow-Origin**. Es la política
  esperada sin CORS habilitado, no un defecto del flujo local mediante proxy.
- React realizó sus GET/POST por `/registry`, `/telemetry` y `/alerts` sin errores CORS;
  también funcionó el proxy local de preview. Las llamadas entre servicios son REST
  servidor a servidor y no están sujetas a CORS del navegador.
- Durante las desconexiones se capturaron nueve respuestas 5xx del **proxy**, sin
  contrato OpenAPI de negocio. Se distinguieron de las respuestas de las APIs y no
  se validaron falsamente como Problem Details de los microservicios.

### Evidencias de esta ejecución

- [Resultados estructurados, mensajes HTTP y snapshots antes/después](evidence/mvp-integration/verification.json).
- [Flujo funcional completado en React](evidence/mvp-integration/flow-completed.png).
- [Historial después de reiniciar servicios y PostgreSQL](evidence/mvp-integration/after-restart.png).
- [Medición confirmada con Alerts no disponible](evidence/mvp-integration/alerts-unavailable.png).

Las capturas no son maquetas; corresponden a los registros anteriores. Al finalizar se
detuvieron los procesos de verificación. No debe esperarse encontrar esos UUID en una
base local distinta a la usada en este ensayo.

## Problemas encontrados y correcciones

- **Reinicio del entorno:** canceló los primeros comandos y eliminó temporales. Se
  reconstruyó únicamente el entorno desechable y se repitieron las verificaciones completas;
  no se reutilizaron resultados interrumpidos como éxitos.
- **Documentación desactualizada:** README y guía todavía describían el frontend como
  base/diagnóstico. Se actualizó ese estado y se enlazó este informe, sin tocar código.
- **No se encontraron defectos funcionales reproducibles** que exigieran modificar
  servicios, frontend o contratos. Las respuestas 400/409/422 y los fallos de conectividad
  fueron casos intencionales con manejo correcto, no errores sin resolver.
- CORS directo no habilitado, PENDING sin reconciliación y falta de credenciales/bases
  del equipo confirmadas son condiciones/limitaciones explícitas, no se ocultaron con
  permisos globales, mocks ni cambios de infraestructura.

## Dependencias pendientes con otros integrantes

- **Docker/Compose:** contextos e imágenes, DNS entre contenedores, bases/usuarios por
  servicio, healthchecks y montaje del archivo externo de reglas. Sin cambios aquí.
- **CI/CD:** ejecutar las suites por componente; PostgreSQL de pruebas con bases
  desechables separadas. No se ejecutan pipelines remotos en esta verificación.
- **Azure/Terraform:** aprovisionamiento, gestión de secretos/configuración, red privada
  y proxy de mismo origen o política CORS explícita para el frontend desplegado.
- **Serverless:** emisor contra POST measurements con UUID real y fecha con zona;
  consumidores deben distinguir registro confirmado de evaluación completada.
- **DevSecOps/aceptación:** seguridad de APIs internas antes de exposición, pruebas
  del entorno desplegado, carga, retención y políticas operativas fuera de este MVP.

La separación física de tablas/bases se verificó; la política de permisos con usuarios
distintos del entorno del equipo no se certificó. El ensayo aislado usó un propietario
local de pruebas, sin reutilizar cuentas ni credenciales productivas. Solo se probó
Chromium, no teléfonos físicos ni compatibilidad exhaustiva entre navegadores.

El flujo local no equivale a un despliegue validado. Permanecen fuera de esta fase
autenticación avanzada, notificaciones, frontend adicional, paginación, recursos cloud,
Kubernetes, Terraform y cambios a workflows o pruebas compartidas.

## Archivos de esta fase y estado final

- Nuevo `docs/architecture/mvp-integration.md` (este informe).
- Nuevos `docs/architecture/evidence/mvp-integration/verification.json` y tres capturas
  del flujo, reinicio y fallo controlado de Alerts.
- `README.md` y `docs/architecture/integration-guide.md`: estado del frontend corregido
  y enlace a este informe. No se sustituyeron los resultados históricos de fases previas.

Una comparación SHA-256 contra los archivos existentes al retomar la ejecución confirmó
que servicios, frontend, contratos, infraestructura, workflows y pruebas compartidas
conservan exactamente su contenido previo. Solo cambiaron los dos documentos indicados;
los demás archivos de esta fase son nuevas evidencias. No se hicieron commits, push,
despliegues ni cambios remotos, y no se descartó trabajo sin commit.

**Estado final: MVP integrado y funcional verificado localmente**, con pruebas completas,
PostgreSQL real, recorrido React y persistencia tras reinicios. El entorno del equipo y
la infraestructura desplegada requieren la configuración y aceptación pendientes arriba,
no nuevas funcionalidades para reproducir este flujo local.
