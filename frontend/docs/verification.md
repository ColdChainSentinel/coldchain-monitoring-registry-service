# Verificación del frontend MVP

Ejecución local del 29 de octubre de 2026 (hora WSL). Node 22.22.0, npm 10.9.4,
React 19.3.1, TypeScript 5.9.3, Vite 7.3.7, Vitest 5.0.5, jsdom 27.4.0.
Los resultados siguientes describen ejecuciones reales, no verificaciones propuestas.

## Comandos del proyecto

Desde `frontend/`:

| Comprobación | Resultado |
|---|---|
| `npm ci` | Instalación limpia desde package-lock correcta |
| `npm run typecheck` | Aprobado |
| `npm test` | **63 pruebas en 10 archivos**, sin fallos |
| `npm run build` | Build Vite aprobado; salida `dist/` |
| `npm audit` | **0 vulnerabilidades reportadas** en el árbol instalado |

Pruebas automatizadas del proyecto:

- Cliente fetch: URL, GET/POST, timeout, errores HTTP, Problem Details, JSON inválido,
  red y un solo envío por registro.
- Clientes de servicios: rutas reales, listas vacías, ausencia de fallback, modelos
  incompatibles, campo type opcional y decimales de PostgreSQL sin redondeo silencioso.
- Validaciones: código/nombre/tipo, temperaturas negativas/cero/coma decimal, precisión
  no representable, UTC y fechas locales inválidas.
- Formularios: validación antes del POST, éxito, 409, errores de campos del servidor,
  bloqueo durante envío y advertencia ante respuestas no confirmadas.
- Estados: PENDING nunca equivale a NO_ALERT; COMPLETED no implica ausencia de alerta;
  una alerta asociada puede coexistir con PENDING almacenado en Telemetry.
- Dashboard: contadores reales por punto, selección, lista tras registrar, APIs caídas
  y protección ante respuestas tardías del punto anterior.
- Alertas: LOW/HIGH, OPEN, límites snapshot, fecha/medición y consulta vacía neutral.

Los fixtures y mocks se importan solo desde tests. El bundle no utiliza datos de
prueba para sustituir fallos de los servicios. No se añadieron dependencias productivas;
Testing Library y jsdom se añadieron únicamente como herramientas de desarrollo.

## Navegador con los tres microservicios reales

Se ejecutó **Chromium 156.0.8078.4** mediante Playwright externo al proyecto. Se usaron
los JAR existentes Java 17, sin perfiles de test, y PostgreSQL **16.6** con tres bases
independientes y desechables. Vite sirvió el frontend en 5173 y sus proxies reenviaron
a Registry 8080, Telemetry 8081 y Alerts 8082. Las solicitudes del navegador **no se
interceptaron ni simularon**.

Recorrido comprobado:

1. Catálogo vacío real: contador cero y llamada a registrar el primer punto.
2. Formulario de punto: validación visible, POST Registry **201**, lista actualizada
   y UUID seleccionado. Repetir código produjo **409** real y mensaje comprensible.
3. Se configuró externamente una regla 2–8 únicamente para ese punto desechable.
   Estos valores son fixtures explícitos de la demostración, no defaults del producto.
4. Desde la UI se registraron 9, 1, 5, 2 y 8 °C. Los cinco POST a Telemetry devolvieron
   **201 COMPLETED**. GET Alerts devolvió exactamente dos alertas, HIGH y LOW, con
   OPEN, UUID de medición y snapshots 2–8. Los límites inclusivos no generaron alertas.
5. Dashboard real: **1 punto, 5 mediciones, 2 alertas, 0 PENDING**. Los tres endpoints
   de salud respondieron UP. Se consultaron las tablas y asociaciones de mediciones.
6. Anchos **1440 px y 390 px**: navegación, formularios y tablas operables, sin
   desbordamiento horizontal del documento. Las tablas usan scroll interno; en móvil
   se indica cómo acceder a todas sus columnas.
7. Se detuvo Alerts. Una nueva medición desde la UI devolvió **201 PENDING** y dejó
   seis mediciones, no una repetición del POST. Se mostró advertencia explícita;
   contador de alertas desconocido `—`, no cero ni NO_ALERT.
8. Se detuvo Registry. La interfaz mostró error de consulta y catálogo no disponible,
   sin convertir ese error en un catálogo vacío o una invitación engañosa de primer uso.
9. **Sin errores JavaScript no capturados** en ninguno de los recorridos anteriores.

Los procesos Java, Vite y PostgreSQL temporales se detuvieron después de verificar.
No se modificaron fuentes/configuración/tests del backend ni archivos fuera de frontend.
No se hicieron commits, push ni despliegues.

## Capturas del recorrido real

Las capturas corresponden a datos creados mediante las APIs sobre bases temporales;
no son maquetas ni contenido precargado en la aplicación. Los UUID ya no representan
datos disponibles al terminar esos procesos de prueba.

| Pantalla | Evidencia |
|---|---|
| Resumen de escritorio, 1440 px | [dashboard-desktop.png](screenshots/dashboard-desktop.png) |
| Resumen móvil, 390 px | [dashboard-mobile.png](screenshots/dashboard-mobile.png) |
| Registro y lista de puntos | [points-desktop.png](screenshots/points-desktop.png) |
| Historial de alertas, escritorio | [alerts-desktop.png](screenshots/alerts-desktop.png) |
| Formulario e historial de mediciones móvil | [measurements-mobile.png](screenshots/measurements-mobile.png) |
| Alertas móvil | [alerts-mobile.png](screenshots/alerts-mobile.png) |
| Registro confirmado con evaluación pendiente | [pending-desktop.png](screenshots/pending-desktop.png) |
| Registry desconectado, móvil | [unavailable-mobile.png](screenshots/unavailable-mobile.png) |

## Cómo repetir el flujo manualmente

1. Arrancar Registry, Telemetry y Alerts con sus bases independientes según sus README.
   Mantener URLs internas correctas. No ejecutar suites sobre bases productivas.
2. `npm ci && npm run dev` desde frontend, abrir http://localhost:5173.
3. En **Puntos**, registrar un punto real y usar su UUID en el archivo externo de
   reglas de Alerts con límites aprobados. Reiniciar Alerts si cambia ese archivo.
4. Seleccionar el punto; en **Mediciones** enviar valores dentro, fuera y exactamente
   en ambos límites. Confirmar resultados en **Alertas** y **Resumen**.
5. Detener Alerts solo en un entorno local de pruebas; registrar otra medición y
   verificar PENDING, no NO_ALERT. No reenviar la medición para reintentar evaluación.
6. Revisar a 390 px desde el modo responsive del navegador, incluyendo el scroll de tablas.

## Límites de esta evidencia

- Se verificó Chromium en escritorio y viewport móvil; no dispositivos físicos,
  Safari/Firefox antiguos, lectores de pantalla ni certificación WCAG completa.
- Las lecturas decimales exactas usan el contexto source de JSON.parse. Un navegador
  sin soporte recibe error explícito al leer números, no redondeo silencioso.
- No se implementaron ni probaron despliegues, contenedores, pipelines remotos o Azure.
- El proxy verificado es el de desarrollo; el equipo de despliegue debe configurar
  mismo origen o CORS y URLs públicas al compilar. No se modificó CORS del backend.
- NO_ALERT no se expone en GET de Telemetry: la UI describe por separado la evaluación
  persistida y el resultado de consultar alertas; no ejecuta evaluaciones para leerlas.
- Los tiempos de cada consulta pueden diferir: no hay snapshot distribuido ni polling.
- No hay paginación en los contratos actuales; listas grandes requerirán evolución
  de API antes de garantizar rendimiento a escala. Esta es una entrega MVP.
