---
name: monitoreo-cadena-frio-hus
description: Backlog funcional (36 historias de usuario, 9 épicas, con definición, título y criterios de aceptación) de una plataforma de monitoreo de temperatura para la cadena de frío en almacenamiento y transporte. Usar siempre que el usuario mencione historias de usuario, HU-XX, EP-XX, criterios de aceptación, backlog, sprint, refinamiento o requisitos de este sistema, o pida casos de prueba, Gherkin, tareas técnicas, estimaciones, modelo de datos, endpoints, matriz de trazabilidad, revisión de consistencia o código basado en estas historias, aunque no nombre la skill. También aplica a temas del dominio como puntos de monitoreo, dispositivos de temperatura, rangos térmicos, alertas por desviación, incidentes, sin comunicación, auditoría, roles (supervisor, conductor, personal técnico) o exportación CSV de mediciones.
---

# Monitoreo de Cadena de Frío — Historias de Usuario

Fuente de verdad del backlog funcional de una plataforma que recibe mediciones de dispositivos de temperatura, las evalúa contra rangos configurables, genera alertas y permite gestionar incidentes durante el almacenamiento y el transporte.

El detalle completo de cada historia (definición, título y criterios de aceptación literales) está en `references/historias-usuario.md`. Léelo siempre que necesites el texto exacto de una HU o de un CA; no reconstruyas criterios de memoria.

## Cómo usar esta skill

1. Identifica las HU o épicas implicadas en la petición con el índice de abajo.
2. Abre `references/historias-usuario.md` y lee solo las HU relevantes (están ordenadas por ID y agrupadas por épica).
3. Cita los criterios por su ID (por ejemplo, "HU-14 / CA-02") para que el resultado sea trazable.
4. Verifica el resultado contra las reglas transversales de la sección siguiente: muchos criterios dependen unos de otros y un entregable correcto para una HU puede violar otra.

Tareas típicas y cómo abordarlas:

- **Casos de prueba / Gherkin**: un escenario por criterio de aceptación como mínimo. Los CA ya vienen en forma Dado/Cuando/Entonces implícita; conserva sus condiciones exactas. Añade casos límite solo si derivan de una regla transversal (por ejemplo, temperatura igual al límite).
- **Tareas técnicas o estimación**: descompón por capa (datos, API, UI, seguridad) y enlaza cada tarea con los CA que cubre. Señala dependencias entre HU (ver sección de dependencias).
- **Modelo de datos o API**: deriva entidades y atributos de los CA. Las reglas de versionado, vigencia temporal y auditoría implican historizar en lugar de sobrescribir.
- **Revisión de calidad del backlog**: busca ambigüedades, criterios no verificables, solapamientos o vacíos entre HU, y proponlo como observaciones, sin modificar los textos originales salvo que se pida.
- **Código**: implementa respetando los CA tal cual; si un criterio es ambiguo, dilo antes de asumir.

Responde en español, que es el idioma del backlog, salvo que el usuario pida otro.

## Glosario del dominio

- **Punto de monitoreo**: lugar donde se controla la temperatura. Tipo: *almacenamiento* o *transporte* (unidad refrigerada).
- **Dispositivo de temperatura**: sensor con identificador único asociado a un punto en cada momento.
- **Medición**: lectura con dispositivo, temperatura, unidad, fecha y hora de medición, y fecha y hora de recepción (distintas entre sí).
- **Rango**: temperatura mínima y máxima por punto, con versiones vigentes en el tiempo.
- **Alerta / evento de desviación**: agrupa mediciones consecutivas fuera de rango de un dispositivo.
- **Incidente**: registro de atención (manual, desde alerta o desde el conductor) con responsable, acciones, estados y cierre documentado.
- **Ámbito de acceso**: puntos sobre los que un usuario tiene permisos; condiciona lo que ve, exporta y recibe.

## Reglas transversales

Estas reglas se repiten en varias HU. Aplícalas siempre; explican el "porqué" de muchos criterios.

1. **Vigencia temporal, no estado actual.** Las mediciones se asocian al punto y se evalúan con el rango vigentes en su *fecha y hora de medición*, no en la de recepción ni en la de consulta (HU-07, HU-10). Las alertas muestran los límites usados originalmente.
2. **Historizar, nunca sobrescribir.** Cambios de rango, reasignaciones, responsables y estados conservan el valor anterior, el autor y la fecha y hora (HU-07, HU-10, HU-30, HU-32, HU-36). Un dato conflictivo no pisa al original (HU-14).
3. **Desactivar no es borrar.** Cuentas, puntos y dispositivos desactivados bloquean nuevos usos, pero su histórico, mediciones, alertas e incidentes siguen consultables y conservan la autoría (HU-03, HU-05, HU-08, HU-34).
4. **Límites inclusivos.** Una temperatura igual al mínimo o al máximo está dentro del rango (HU-09/CA-03). El mínimo debe ser estrictamente menor que el máximo.
5. **La ausencia de datos nunca es "normal".** Sin mediciones, sin rango, datos desactualizados o sin comunicación se muestran de forma explícita. Tampoco cuentan como recuperación de una alerta (HU-15, HU-16, HU-21, HU-26).
6. **Los datos rechazados no contaminan el monitoreo.** Mediciones inválidas, de dispositivos desconocidos o inactivos se rechazan con motivo registrado y no alteran el dashboard ni generan alertas (HU-08, HU-13).
7. **Idempotencia y orden.** Un mismo identificador de medición no crea una segunda medición ni alerta; una medición anterior a la última conocida entra al historial sin reemplazar la temperatura más reciente (HU-14).
8. **Estados independientes entre alerta e incidente.** Reconocer una alerta no la resuelve (HU-24); la recuperación térmica no cierra el incidente ni declara el producto apto (HU-21); cerrar un incidente con desviación activa exige justificación y no desactiva la alerta (HU-33).
9. **Autorización en el servidor.** Los permisos se aplican a cada solicitud, incluso por enlace directo, y los cambios de permisos rigen desde la siguiente solicitud (HU-02). Dashboard, avisos, consultas y exportaciones se limitan al ámbito del usuario (HU-16, HU-22, HU-35).
10. **Autenticación sin filtrar información.** Los fallos de inicio de sesión no revelan qué dato es incorrecto (HU-01/CA-02).
11. **Trazabilidad de autor.** Toda acción relevante guarda identidad y fecha y hora; la fecha del hecho es distinta de la fecha de registro (HU-28).
12. **Auditoría de solo lectura para operativos.** El registro de cambios no puede modificarse ni eliminarse desde la plataforma (HU-36/CA-03).

## Índice de historias

| ID | Título | Épica |
|----|--------|-------|
| HU-01 | Iniciar y cerrar sesión | EP-01 Acceso y permisos |
| HU-02 | Asignar roles y niveles de acceso | EP-01 |
| HU-03 | Gestionar el estado de las cuentas de usuario | EP-01 |
| HU-04 | Registrar puntos de monitoreo | EP-02 Gestión de puntos y dispositivos |
| HU-05 | Actualizar puntos de monitoreo | EP-02 |
| HU-06 | Registrar dispositivos de temperatura | EP-02 |
| HU-07 | Reasignar un dispositivo a otro punto | EP-02 |
| HU-08 | Desactivar dispositivos retirados de operación | EP-02 |
| HU-09 | Configurar temperaturas mínimas y máximas por punto | EP-03 Configuración del monitoreo |
| HU-10 | Conservar versiones de los rangos configurados | EP-03 |
| HU-11 | Configurar el tiempo máximo sin recibir datos | EP-03 |
| HU-12 | Recibir y almacenar mediciones de forma centralizada | EP-04 Recepción y calidad de mediciones |
| HU-13 | Identificar mediciones inválidas | EP-04 |
| HU-14 | Controlar mediciones duplicadas y fuera de orden | EP-04 |
| HU-15 | Reconocer la pérdida y recuperación de comunicación | EP-04 |
| HU-16 | Consultar el dashboard general | EP-05 Visualización e históricos |
| HU-17 | Consultar el detalle de un punto | EP-05 |
| HU-18 | Consultar el historial de temperaturas | EP-05 |
| HU-19 | Detectar desviaciones de temperatura | EP-06 Gestión de alertas |
| HU-20 | Agrupar mediciones de una misma desviación | EP-06 |
| HU-21 | Identificar la recuperación de temperatura | EP-06 |
| HU-22 | Recibir avisos visibles de nuevas alertas | EP-06 |
| HU-23 | Buscar y filtrar alertas | EP-06 |
| HU-24 | Reconocer una alerta | EP-06 |
| HU-25 | Asignar una unidad refrigerada a un conductor | EP-07 Monitoreo durante el transporte |
| HU-26 | Consultar el estado de la unidad asignada | EP-07 |
| HU-27 | Comunicar una incidencia desde la unidad | EP-07 |
| HU-28 | Registrar un incidente manualmente | EP-08 Gestión de incidentes |
| HU-29 | Crear o vincular un incidente desde una alerta | EP-08 |
| HU-30 | Asignar un responsable a cada incidente | EP-08 |
| HU-31 | Registrar las acciones realizadas en un incidente | EP-08 |
| HU-32 | Marcar el inicio de atención de un incidente | EP-08 |
| HU-33 | Cerrar un incidente documentando el resultado | EP-08 |
| HU-34 | Consultar incidentes y su historial | EP-08 |
| HU-35 | Exportar las mediciones consultadas | EP-09 Exportación y auditoría |
| HU-36 | Consultar el registro de cambios (auditoría) | EP-09 |

## Dependencias entre épicas

Útil para ordenar sprints y detectar bloqueos:

- **EP-01** es base de todo: sin autenticación y permisos no hay ámbito de acceso para dashboard, avisos ni exportación.
- **EP-02 → EP-03 → EP-04**: no se pueden evaluar mediciones sin dispositivos asociados a puntos ni sin rangos y umbrales de comunicación.
- **EP-04 → EP-05 y EP-06**: dashboard, históricos y alertas consumen mediciones válidas y deduplicadas.
- **EP-06 → EP-08**: los incidentes pueden crearse desde alertas (HU-29), pero también existen de forma independiente (HU-28).
- **EP-07** combina asignaciones (EP-01/EP-02), estado de la unidad (EP-04/EP-06) e incidentes (EP-08).
- **EP-09** transversal: la exportación depende de EP-05 y la auditoría de los cambios de EP-01, EP-02, EP-03 y EP-08.

## Formato al presentar historias

Cuando el usuario pida mostrar o reescribir una HU, usa esta estructura (la misma de la referencia):

```
### HU-XX — Título
**Épica:** EP-XX — Nombre
**Definición:** Como [rol], quiero [acción], para [beneficio].
**Criterios de aceptación:**
- CA-01: ...
```

Al proponer HU nuevas o ajustes, mantén el estilo del backlog: rol explícito, beneficio verificable y criterios en forma "Cuando… entonces…" que describan comportamiento observable, un criterio por regla.