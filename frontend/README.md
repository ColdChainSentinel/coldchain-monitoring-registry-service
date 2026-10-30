# ColdChain Sentinel — frontend MVP

React 19 + TypeScript + Vite. Interfaz en español, responsive, sin librería visual,
router, gestor de estado ni cliente HTTP adicional. Consume exclusivamente las APIs
reales; no hay modo demo, registros precargados, gráficas ficticias ni fallback a mocks.

## Ejecutar

Recomendado Node **22.22+ LTS** y npm (mínimo declarado del proyecto: 22.12).
Desde `frontend/`:

```sh
npm ci
npm run dev
```

Abrir http://localhost:5173. El servidor escucha en loopback; para revisar el diseño
móvil se puede usar el modo responsive del navegador. Requiere los servicios locales
para operar, pero muestra errores explícitos si no están disponibles.

```sh
npm run typecheck
npm test
npm run build
npm run preview
```

`build` produce `dist/`. `preview` sirve el bundle para revisión local, no es un
servidor de producción. No se implementan contenedores, despliegues ni configuración cloud.

## URLs y variables públicas

Las variables existentes se conservan. Opcionalmente copiar `.env.example` a
`.env.local`, que Git ignora. Reiniciar Vite después de modificarlas:

| Variable | Valor predeterminado | Proxy local de desarrollo |
|---|---|---|
| `VITE_REGISTRY_BASE_URL` | `/registry` | `http://localhost:8080` |
| `VITE_TELEMETRY_BASE_URL` | `/telemetry` | `http://localhost:8081` |
| `VITE_ALERTS_BASE_URL` | `/alerts` | `http://localhost:8082` |

Los prefijos se eliminan al reenviar, preservando las rutas reales. No se modificó
CORS del backend. En un despliegue, el responsable debe proporcionar proxy de mismo
origen o URLs absolutas y CORS explícito. Las variables `VITE_*` son **públicas y de
compilación**, no configuración secreta de servidor; nunca incluir contraseñas o tokens.

Timeout frontend: **20 segundos**, para permitir las llamadas encadenadas del backend.
Se cancelan consultas obsoletas al cambiar de punto, pero **no se reintentan POST**.
Si se pierde una respuesta de registro, la interfaz pide actualizar la lista antes
de volver a enviar: el backend podría haber confirmado el registro.

## Pantallas y alcance real de los indicadores

- **Resumen:** total de puntos del catálogo, cantidad de mediciones y alertas **del
  punto seleccionado**, cantidad de PENDING persistidos, última medición y consultas
  recientes. Contadores desconocidos muestran `—`, nunca un cero de respaldo.
- **Puntos:** catálogo, selección y registro con código único, nombre y tipo.
  Valida ASCII/código de hasta 40, nombre obligatorio de hasta 120 y los dos enums
  reales. Conflictos 409 y Problem Details se muestran sin perder lo introducido.
- **Mediciones:** selección de punto real, entrada manual en Celsius, fecha/hora
  local convertida a UTC, respuesta de registro e historial cronológico inverso.
  Incluye identificador, evaluación y asociación consultada con alertas.
- **Alertas:** tipo LOW/HIGH cuando esté informado, temperatura, fecha de creación,
  estado OPEN y límites snapshot. Muestra `Tipo no informado` si falta el campo opcional,
  sin inventarlo. No hay edición de reglas, cierre ni notificaciones.

Las consultas son manuales/al entrar o cambiar de punto y tras registros. No se
promete actualización en tiempo real. Los datos antiguos que quedan visibles tras
un error de refresco están acompañados de una advertencia y fecha de última consulta.
Las tablas invierten el orden ascendente garantizado por el backend (fecha y UUID),
sin reordenar timestamps de microsegundos mediante Date de JavaScript.

### Evaluación no equivale a ausencia de alertas

Telemetry expone COMPLETED, PENDING y RULE_NOT_CONFIGURED, **no NO_ALERT**. El frontend
no llama POST `/api/alerts/evaluate` para consultar resultados, pues eso sería una
mutación. Presenta:

- **COMPLETED / Evaluada:** se completó la evaluación, con o sin alerta.
- **PENDING / Evaluación pendiente:** la medición está guardada; no implica NO_ALERT.
- **RULE_NOT_CONFIGURED / Sin regla térmica:** faltan límites explícitos por UUID;
  no se inventan umbrales ni se configura la regla desde la UI.
- **Sin alerta registrada:** únicamente si COMPLETED y una consulta exitosa de Alerts
  para ese punto no incluye el measurementId. Describe esa consulta, no afirma un
  estado NO_ALERT que el endpoint de Telemetry no devuelve.

Si Alerts no responde, la asociación queda **no confirmada**, incluso para COMPLETED.
Si un reintento externo produjo una alerta pero Telemetry sigue PENDING, ambos hechos
se muestran por separado. Una lista vacía de alertas nunca significa que todo esté normal.

## Endpoints utilizados

| Servicio | Método y ruta |
|---|---|
| Registry | GET / POST `/api/v1/monitoring-points` |
| Telemetry | POST `/api/measurements` |
| Telemetry | GET `/api/measurements?monitoringPointId={uuid}` |
| Alerts | GET `/api/alerts?monitoringPointId={uuid}` |
| Los tres | GET `/actuator/health` |

No se inventaron endpoints de resumen global, evaluación GET, reglas, edición ni borrado.
Los modelos se verifican en ejecución; una respuesta incompatible produce error, no datos
vacíos o parciales. Los contratos permanecen sin cambios.

## Decimales y fechas

Las mediciones introducidas manualmente aceptan punto o coma decimal, negativos y cero.
Antes de enviar un Number se comprueba que el valor decimal pueda hacer round-trip JSON
sin cambiarlo. Si no, se rechaza explícitamente en vez de alterar una medición sensible.
Para valores de precisión superior se debe usar un emisor que preserve BigDecimal.

Al leer JSON se utiliza el contexto `source` de `JSON.parse` de navegadores modernos.
Los números que exceden Number se conservan como tokens decimales exactos, sin una
dependencia matemática nueva. Así una alerta a 8.000000000000000001 no se muestra
falsamente como 8, ni un exponente pequeño se redondea a cero. No se acepta una
temperatura string enviada por una API incompatible. Navegadores sin ese contexto
reciben un error explícito al leer números, no una aproximación silenciosa.

La fecha manual tiene precisión de segundos y zona local del navegador (indicada en
el formulario); se envía RFC 3339 UTC. Fechas mostradas se localizan con Intl, con
el timestamp completo original en el atributo title del elemento time.

## Estructura

```text
src/
  api/                 # fetch, Problem Details y URLs configurables existentes
  services/            # monitoring.ts, telemetry.ts, alerts.ts (solo lectura)
  types/               # Modelos del contrato
  lib/                 # Validaciones, modelos en ejecución, decimales y formatos
  hooks/               # Consultas cancelables y protección ante respuestas tardías
  components/          # Iconos SVG, feedback, salud de APIs
  features/
    monitoring/        # Formulario y lista de puntos
    telemetry/         # Formulario, historial, evaluación y confirmación de registro
    alerts/            # Tabla de alertas y snapshots
  pages/HomePage.tsx   # Navegación y composición del dashboard
  test/                # Setup y fixtures exclusivamente para pruebas
  styles.css           # Diseño responsive, foco y reducción de movimiento
docs/                  # Verificación y capturas del flujo real
```

## Pruebas y límites

Vitest existente, más Testing Library, user-event, jest-dom y jsdom **solo en desarrollo**.
Pruebas de formularios, errores, clientes/rutas, validaciones, decimales, estados,
dashboard, registros y consultas que cambian de punto sin mezclar respuestas.
Los mocks viven únicamente en tests y nunca se importan desde componentes productivos.

No hay autenticación avanzada, persistencia local de credenciales, modificaciones de
reglas, reintentos de evaluación, reconciliación de PENDING ni paginación (no existe
en estos contratos). No hay certificación WCAG ni pruebas en teléfonos físicos.
Las consultas de alertas y mediciones son independientes, no una transacción distribuida.

Resultados ejecutados y capturas: [verificación](docs/verification.md).
