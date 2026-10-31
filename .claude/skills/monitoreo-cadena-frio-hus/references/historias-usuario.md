# Historias de usuario — Monitoreo de Cadena de Frío

## Tabla de contenido

- EP-01 — Acceso y permisos: HU-01, HU-02, HU-03
- EP-02 — Gestión de puntos y dispositivos: HU-04, HU-05, HU-06, HU-07, HU-08
- EP-03 — Configuración del monitoreo: HU-09, HU-10, HU-11
- EP-04 — Recepción y calidad de mediciones: HU-12, HU-13, HU-14, HU-15
- EP-05 — Visualización e históricos: HU-16, HU-17, HU-18
- EP-06 — Gestión de alertas: HU-19, HU-20, HU-21, HU-22, HU-23, HU-24
- EP-07 — Monitoreo durante el transporte: HU-25, HU-26, HU-27
- EP-08 — Gestión de incidentes: HU-28, HU-29, HU-30, HU-31, HU-32, HU-33, HU-34
- EP-09 — Exportación y auditoría: HU-35, HU-36

---

# EP-01 — Acceso y permisos

### HU-01 — Iniciar y cerrar sesión
**Épica:** EP-01 — Acceso y permisos
**Definición:** Como usuario, quiero iniciar y cerrar sesión, para acceder a mis funciones dentro de la aplicación.
**Criterios de aceptación:**
- CA-01: Dada una cuenta activa, cuando ingresa credenciales correctas, entonces el sistema muestra un mensaje de inicio de sesión exitoso.
- CA-02: Cuando ingreso credenciales incorrectas o utilizo una cuenta inactiva, entonces el sistema rechaza el acceso sin revelar cuál de los datos es incorrecto.
- CA-03: Cuando cierro sesión, entonces las funciones protegidas requieren una nueva autenticación.

### HU-02 — Asignar roles y niveles de acceso
**Épica:** EP-01 — Acceso y permisos
**Definición:** Como administrador, quiero asignar roles y niveles de accesos a las cuentas registradas, para evitar accesos no autorizados.
**Criterios de aceptación:**
- CA-01: Cuando asigno un rol y puntos de monitoreo a un usuario, entonces el sistema guarda y muestra sus permisos efectivos.
- CA-02: Cuando un usuario intenta una operación o consulta fuera de sus permisos, entonces el sistema la rechaza, incluso si utiliza un enlace directo.
- CA-03: Cuando modifico permisos, entonces el cambio se aplica a las siguientes solicitudes del usuario.

### HU-03 — Gestionar el estado de las cuentas de usuario
**Épica:** EP-01 — Acceso y permisos
**Definición:** Como administrador, quiero manejar el estado de las cuentas del usuario, para la gestión segura de accesos.
**Criterios de aceptación:**
- CA-01: Cuando registro una cuenta con los datos obligatorios y un identificador de acceso único, entonces queda disponible con el rol asignado.
- CA-02: Cuando desactivo una cuenta, entonces se bloquean sus nuevos accesos y sus sesiones existentes dejan de permitir operaciones protegidas.
- CA-03: Cuando una cuenta se desactiva, entonces sus acciones e incidentes anteriores conservan la identificación de su autor.

---

# EP-02 — Gestión de puntos y dispositivos

### HU-04 — Registrar puntos de monitoreo
**Épica:** EP-02 — Gestión de puntos y dispositivos
**Definición:** Como personal técnico, quiero registrar puntos de monitoreo, para identificar los lugares donde se controla la temperatura.
**Criterios de aceptación:**
- CA-01: Cuando registro un código único, nombre y tipo de punto —almacenamiento o transporte—, entonces el sistema crea el punto y lo muestra en el listado.
- CA-02: Cuando el código ya existe, entonces el sistema rechaza el registro e informa la duplicidad.
- CA-03: Cuando falta un dato obligatorio, entonces el sistema identifica el campo y no guarda un registro incompleto.

### HU-05 — Actualizar puntos de monitoreo
**Épica:** EP-02 — Gestión de puntos y dispositivos
**Definición:** Como personal técnico, quiero actualizar puntos de monitoreo, para reflejar cambios operativos sin perder registros anteriores.
**Criterios de aceptación:**
- CA-01: Cuando modifico los datos descriptivos de un punto, entonces el sistema conserva su identificador y muestra la información actualizada.
- CA-02: Cuando intento desactivar un punto con dispositivos activos asociados, entonces el sistema exige reasignarlos o desactivarlos previamente.
- CA-03: Cuando desactivo un punto, entonces deja de admitir nuevas asociaciones y sus históricos permanecen consultables.

### HU-06 — Registrar dispositivos de temperatura
**Épica:** EP-02 — Gestión de puntos y dispositivos
**Definición:** Como personal técnico, quiero registrar dispositivos de temperatura, para identificar el origen de las mediciones recibidas.
**Criterios de aceptación:**
- CA-01: Cuando registro un identificador único y selecciono un punto activo, entonces el sistema crea el dispositivo y guarda su asociación.
- CA-02: Cuando intento registrar un identificador existente, entonces el sistema impide la duplicación.
- CA-03: Cuando consulto el dispositivo, entonces puedo ver su identificador, estado y punto asociado.

### HU-07 — Reasignar un dispositivo a otro punto
**Épica:** EP-02 — Gestión de puntos y dispositivos
**Definición:** Como personal técnico, quiero reasignar un dispositivo a otro punto, para mantener la trazabilidad al momento de cambiar la ubicación.
**Criterios de aceptación:**
- CA-01: Cuando selecciono un nuevo punto activo y confirmo el cambio, entonces se registra la nueva asociación con su fecha y hora de vigencia.
- CA-02: Cuando consulto mediciones anteriores al cambio, entonces conservan el punto al que pertenecían.
- CA-03: Cuando llega una medición retrasada, entonces se vincula al punto vigente en su fecha y hora de medición; si no existe una asociación válida, queda identificada como no asociable.

### HU-08 — Desactivar dispositivos retirados de operación
**Épica:** EP-02 — Gestión de puntos y dispositivos
**Definición:** Como personal técnico, quiero desactivar dispositivos retirados de operación, para inhabilitar su funcionamiento.
**Criterios de aceptación:**
- CA-01: Cuando desactivo un dispositivo, entonces el sistema registra el cambio y lo identifica como inactivo.
- CA-02: Cuando llegan datos de un dispositivo inactivo, entonces no actualizan el estado operativo del punto y se registra el motivo de rechazo.
- CA-03: Cuando consulto un dispositivo inactivo, entonces sus mediciones y alertas anteriores siguen disponibles.

---

# EP-03 — Configuración del monitoreo

### HU-09 — Configurar temperaturas mínimas y máximas por punto
**Épica:** EP-03 — Configuración del monitoreo
**Definición:** Como supervisor, quiero configurar temperaturas mínimas y máximas por punto, para evaluar si las mediciones cumplen las condiciones de conservación.
**Criterios de aceptación:**
- CA-01: Cuando ingreso límites numéricos con el mínimo menor que el máximo, entonces el sistema guarda el rango y muestra la unidad utilizada.
- CA-02: Cuando el mínimo es mayor o igual que el máximo, entonces el sistema rechaza la configuración.
- CA-03: Cuando una temperatura coincide exactamente con alguno de los límites, entonces se considera dentro del rango.

### HU-10 — Conservar versiones de los rangos configurados
**Épica:** EP-03 — Configuración del monitoreo
**Definición:** Como supervisor, quiero conservar las versiones de los rangos configurados, para conocer qué condiciones se aplicaron en cada momento.
**Criterios de aceptación:**
- CA-01: Cuando modifico un rango, entonces se conserva la versión anterior y se registra el autor y la fecha y hora de vigencia de la nueva versión.
- CA-02: Cuando se evalúa una medición, entonces se utiliza el rango vigente en su fecha y hora de medición.
- CA-03: Cuando consulto una alerta anterior, entonces se muestran los límites utilizados originalmente, aunque el rango actual sea diferente.

### HU-11 — Configurar el tiempo máximo sin recibir datos
**Épica:** EP-03 — Configuración del monitoreo
**Definición:** Como supervisor, quiero configurar el tiempo máximo sin recibir datos de cada dispositivo, para detectar interrupciones del monitoreo.
**Criterios de aceptación:**
- CA-01: Cuando ingreso un intervalo positivo con su unidad de tiempo, entonces el sistema guarda la configuración.
- CA-02: Cuando ingreso un valor nulo, cero o negativo, entonces el sistema rechaza el cambio.
- CA-03: Cuando un dispositivo supera el intervalo desde su última recepción válida —o desde su activación si nunca transmitió—, entonces se identifica como "Sin comunicación".

---

# EP-04 — Recepción y calidad de mediciones

### HU-12 — Recibir y almacenar mediciones de forma centralizada
**Épica:** EP-04 — Recepción y calidad de mediciones
**Definición:** Como supervisor, quiero que las mediciones se reciban y almacenen de forma centralizada, para contar con información trazable de la cadena de frío.
**Criterios de aceptación:**
- CA-01: Cuando un dispositivo activo envía una medición válida, entonces se almacenan su identificador, temperatura, unidad, fecha y hora de medición y punto asociado.
- CA-02: Cuando el sistema recibe la medición, entonces también registra la fecha y hora de recepción.
- CA-03: Cuando el registro termina correctamente, entonces la medición queda disponible en las consultas correspondientes.

### HU-13 — Identificar mediciones inválidas
**Épica:** EP-04 — Recepción y calidad de mediciones
**Definición:** Como personal técnico, quiero identificar mediciones inválidas, para evitar que datos incorrectos alteren el monitoreo.
**Criterios de aceptación:**
- CA-01: Cuando una medición carece de dispositivo, temperatura numérica, fecha y hora válidas o una unidad admitida, entonces se rechaza como medición operativa.
- CA-02: Cuando el dispositivo es desconocido, entonces sus datos no se incorporan al historial operativo.
- CA-03: Cuando se rechaza una medición, entonces se registra el motivo y esta no modifica el dashboard ni genera alertas de temperatura.

### HU-14 — Controlar mediciones duplicadas y fuera de orden
**Épica:** EP-04 — Recepción y calidad de mediciones
**Definición:** Como supervisor, quiero que el sistema controle mediciones duplicadas y recibidas fuera de orden, para mantener un historial consistente.
**Criterios de aceptación:**
- CA-01: Dado un identificador de medición ya procesado para un dispositivo, cuando se recibe el mismo registro, entonces no se crea una segunda medición ni una segunda alerta por ese registro.
- CA-02: Cuando llega una medición válida con fecha anterior a la última conocida, entonces se incorpora al historial sin reemplazar la temperatura más reciente del dashboard.
- CA-03: Cuando llega el mismo identificador con contenido diferente, entonces se registra un conflicto y no se sobrescribe el dato original.

### HU-15 — Reconocer la pérdida y recuperación de comunicación
**Épica:** EP-04 — Recepción y calidad de mediciones
**Definición:** Como responsable de operaciones, quiero reconocer cuándo un dispositivo deja de transmitir y cuándo vuelve a hacerlo, para distinguir una falta de datos de una condición térmica normal.
**Criterios de aceptación:**
- CA-01: Cuando se supera el intervalo configurado sin recepción válida, entonces el sistema muestra "Sin comunicación" y conserva visible la fecha de la última medición.
- CA-02: Cuando un dispositivo transmite pero su medición más reciente sigue siendo antigua, entonces el sistema distingue "Datos desactualizados" de una lectura actual.
- CA-03: Cuando vuelve a recibirse una medición válida y actual, entonces se restablece el estado de comunicación y se registra la recuperación.

---

# EP-05 — Visualización e históricos

### HU-16 — Consultar el dashboard general
**Épica:** EP-05 — Visualización e históricos
**Definición:** Como supervisor o responsable de almacén, quiero consultar un dashboard general, para identificar los puntos que requieren atención.
**Criterios de aceptación:**
- CA-01: Cuando abro el dashboard, entonces veo los puntos de mi ámbito con temperatura más reciente por dispositivo, fecha y hora y estado de monitoreo.
- CA-02: Cuando un dispositivo no tiene mediciones, no tiene rango o presenta información desactualizada, entonces esa condición se muestra expresamente y no se representa como normal.
- CA-03: Cuando un punto tiene varios dispositivos, entonces el resumen señala si alguno está fuera del rango o presenta problemas de datos y permite identificarlo.

### HU-17 — Consultar el detalle de un punto
**Épica:** EP-05 — Visualización e históricos
**Definición:** Como responsable de operaciones, quiero consultar el detalle de un punto, para revisar sus dispositivos y las incidencias relacionadas.
**Criterios de aceptación:**
- CA-01: Cuando selecciono un punto, entonces veo sus dispositivos, últimas mediciones, rangos aplicables y estados.
- CA-02: Cuando el punto tiene alertas o incidentes, entonces puedo acceder a sus registros desde el detalle.
- CA-03: Cuando actualizo la vista, entonces se muestran los últimos datos almacenados y la hora de actualización de la consulta.

### HU-18 — Consultar el historial de temperaturas
**Épica:** EP-05 — Visualización e históricos
**Definición:** Como supervisor, quiero consultar el historial de temperaturas por dispositivo, punto y período, para analizar el comportamiento térmico.
**Criterios de aceptación:**
- CA-01: Cuando selecciono filtros y un período válido, entonces el sistema muestra únicamente los registros correspondientes, ordenados por fecha y hora de medición.
- CA-02: Cuando reviso los resultados, entonces puedo identificar temperatura, unidad, dispositivo, punto, fecha y hora y rango aplicable, si existía.
- CA-03: Cuando la fecha inicial es posterior a la final, entonces la consulta se rechaza; si el período es válido pero no hay registros, entonces se informa que no existen resultados.

---

# EP-06 — Gestión de alertas

### HU-19 — Detectar desviaciones de temperatura
**Épica:** EP-06 — Gestión de alertas
**Definición:** Como supervisor, quiero que se detecten desviaciones de temperatura, para reconocer condiciones que puedan comprometer los productos.
**Criterios de aceptación:**
- CA-01: Cuando una medición válida es inferior al mínimo o superior al máximo aplicable, entonces el sistema la identifica como desviación y genera o actualiza la alerta correspondiente.
- CA-02: Cuando se genera una alerta, entonces incluye dispositivo, punto, temperatura, límites utilizados y fecha y hora de detección.
- CA-03: Cuando no existe un rango aplicable, entonces la medición se marca como no evaluable y no se genera una alerta de desviación basada en límites inexistentes.

### HU-20 — Agrupar mediciones de una misma desviación
**Épica:** EP-06 — Gestión de alertas
**Definición:** Como responsable de operaciones, quiero agrupar las mediciones de una misma desviación, para evitar alertas repetidas por un evento continuo.
**Criterios de aceptación:**
- CA-01: Dada una alerta activa de un dispositivo, cuando llegan nuevas mediciones consecutivas fuera del rango y bajo la misma configuración, entonces se actualiza el mismo evento.
- CA-02: Cuando consulto el evento, entonces veo su inicio, última medición asociada y temperaturas mínima y máxima registradas.
- CA-03: Cuando hubo una recuperación y posteriormente ocurre otra desviación, entonces se crea un evento nuevo.

### HU-21 — Identificar la recuperación de temperatura
**Épica:** EP-06 — Gestión de alertas
**Definición:** Como supervisor, quiero identificar la recuperación de temperatura, para conocer cuándo terminó una desviación.
**Criterios de aceptación:**
- CA-01: Dada una desviación activa, cuando llega una medición posterior dentro del rango aplicable, entonces el sistema registra la recuperación y su fecha y hora.
- CA-02: Cuando dejan de llegar mediciones, entonces la alerta no se considera recuperada por ausencia de datos.
- CA-03: Cuando se registra una recuperación, entonces no se cierra automáticamente el incidente asociado ni se declara que el producto está apto para su uso.

### HU-22 — Recibir avisos visibles de nuevas alertas
**Épica:** EP-06 — Gestión de alertas
**Definición:** Como supervisor, responsable de almacén o conductor, quiero recibir avisos visibles de nuevas alertas, para reconocer que debo revisar una desviación.
**Criterios de aceptación:**
- CA-01: Cuando se genera una alerta de un punto dentro de mi ámbito, entonces aparece un aviso en la plataforma con el punto, la temperatura y la fecha y hora.
- CA-02: Cuando selecciono el aviso, entonces accedo al detalle de la alerta correspondiente.
- CA-03: Cuando el evento recibe nuevas mediciones sin convertirse en un evento nuevo, entonces el aviso existente se actualiza sin generar una nueva entrada por cada medición.

### HU-23 — Buscar y filtrar alertas
**Épica:** EP-06 — Gestión de alertas
**Definición:** Como responsable de operaciones, quiero buscar y filtrar alertas, para priorizar las desviaciones que necesitan atención.
**Criterios de aceptación:**
- CA-01: Cuando filtro por punto, dispositivo, período, condición activa o recuperada y reconocimiento, entonces se muestran solo las alertas coincidentes.
- CA-02: Cuando consulto el listado sin un orden personalizado, entonces se presenta de la detección más reciente a la más antigua.
- CA-03: Cuando no existen coincidencias, entonces se muestra un mensaje de ausencia de resultados y se permite modificar los filtros.

### HU-24 — Reconocer una alerta
**Épica:** EP-06 — Gestión de alertas
**Definición:** Como responsable de operaciones, quiero reconocer una alerta, para dejar constancia de que fue revisada por una persona.
**Criterios de aceptación:**
- CA-01: Cuando reconozco una alerta, entonces se registra mi identidad y la fecha y hora de reconocimiento.
- CA-02: Cuando una alerta ya fue reconocida, entonces el sistema muestra quién lo hizo y cuándo, sin sobrescribir el registro original.
- CA-03: Cuando reconozco la alerta, entonces su condición térmica permanece activa o recuperada según las mediciones; reconocerla no equivale a resolverla.

---

# EP-07 — Monitoreo durante el transporte

### HU-25 — Asignar una unidad refrigerada a un conductor
**Épica:** EP-07 — Monitoreo durante el transporte
**Definición:** Como supervisor de transporte, quiero asignar una unidad refrigerada a un conductor, para que consulte la información de la carga que está transportando.
**Criterios de aceptación:**
- CA-01: Cuando selecciono un conductor activo y una unidad registrada para transporte, entonces el sistema guarda la asignación y su vigencia.
- CA-02: Cuando el conductor accede al monitoreo, entonces puede identificar y seleccionar sus unidades asignadas.
- CA-03: Cuando finalizo una asignación, entonces se conserva el historial y el acceso operativo del conductor se actualiza conforme a sus permisos restantes.

### HU-26 — Consultar el estado de la unidad asignada
**Épica:** EP-07 — Monitoreo durante el transporte
**Definición:** Como conductor, quiero consultar el estado de la unidad asignada, para conocer la condición de la cadena de frío durante el trayecto.
**Criterios de aceptación:**
- CA-01: Cuando selecciono una unidad asignada, entonces veo sus últimas temperaturas, límites permitidos y fechas y horas de medición.
- CA-02: Cuando existe una desviación, entonces la vista identifica el dispositivo afectado y permite abrir la alerta.
- CA-03: Cuando la información está desactualizada o no hay comunicación, entonces se indica esa condición sin presentar la última temperatura como una lectura actual.

### HU-27 — Comunicar una incidencia desde la unidad
**Épica:** EP-07 — Monitoreo durante el transporte
**Definición:** Como conductor, quiero comunicar una incidencia desde la vista de mi unidad, para que operaciones conozca lo ocurrido y pueda darle seguimiento.
**Criterios de aceptación:**
- CA-01: Cuando inicio el registro desde una unidad, entonces el formulario incorpora su referencia y la alerta seleccionada, si corresponde.
- CA-02: Cuando completo la descripción y la fecha y hora del hecho, entonces se crea el incidente y queda visible para el personal de operaciones autorizado.
- CA-03: Cuando el registro falla por falta de conexión, entonces se informa que no fue enviado y no se muestra una confirmación de recepción.

---

# EP-08 — Gestión de incidentes

### HU-28 — Registrar un incidente manualmente
**Épica:** EP-08 — Gestión de incidentes
**Definición:** Como responsable de almacén o de operaciones, quiero registrar un incidente manualmente, para documentar problemas detectados aunque no exista una alerta automática.
**Criterios de aceptación:**
- CA-01: Cuando selecciono un punto e ingreso descripción y fecha y hora del hecho, entonces se crea un incidente con identificador único y estado "Abierto".
- CA-02: Cuando el incidente se guarda, entonces se registra el autor y la fecha y hora de creación, diferenciadas de la fecha del hecho.
- CA-03: Cuando falta un dato obligatorio, entonces se indica el campo pendiente y no se crea un registro incompleto.

### HU-29 — Crear o vincular un incidente desde una alerta
**Épica:** EP-08 — Gestión de incidentes
**Definición:** Como responsable de operaciones, quiero crear o vincular un incidente desde una alerta, para conectar la desviación detectada con su atención.
**Criterios de aceptación:**
- CA-01: Cuando creo un incidente desde una alerta, entonces el sistema conserva la referencia a la alerta, el dispositivo y el punto.
- CA-02: Cuando la alerta ya está vinculada a un incidente, entonces el sistema muestra el vínculo antes de permitir crear otro registro.
- CA-03: Cuando vinculo una alerta a un incidente existente, entonces la relación es consultable desde ambos registros y no se modifica la medición original.

### HU-30 — Asignar un responsable a cada incidente
**Épica:** EP-08 — Gestión de incidentes
**Definición:** Como supervisor, quiero asignar un responsable a cada incidente, para establecer quién debe coordinar su atención.
**Criterios de aceptación:**
- CA-01: Cuando selecciono un usuario activo con permisos sobre el punto afectado, entonces queda registrado como responsable.
- CA-02: Cuando reasigno el incidente, entonces se conserva el responsable anterior, el nuevo responsable, el autor y la fecha y hora del cambio.
- CA-03: Cuando el responsable consulta sus incidentes, entonces el incidente asignado aparece en su listado.

### HU-31 — Registrar las acciones realizadas en un incidente
**Épica:** EP-08 — Gestión de incidentes
**Definición:** Como responsable de un incidente, quiero registrar las acciones realizadas, para mantener evidencia de la intervención.
**Criterios de aceptación:**
- CA-01: Cuando ingreso una descripción de la acción y confirmo el registro, entonces se guarda con mi identidad y la fecha y hora.
- CA-02: Cuando agrego otra acción, entonces se conserva el contenido de las acciones anteriores.
- CA-03: Cuando intento registrar una acción sin descripción, entonces el sistema rechaza el registro y solicita completar el campo.

### HU-32 — Marcar el inicio de atención de un incidente
**Épica:** EP-08 — Gestión de incidentes
**Definición:** Como responsable de operaciones, quiero marcar el inicio de la atención de un incidente, para diferenciar los casos pendientes de los que ya están siendo atendidos.
**Criterios de aceptación:**
- CA-01: Dado un incidente abierto con responsable asignado, cuando se inicia su atención, entonces cambia a "En atención".
- CA-02: Cuando se cambia el estado, entonces se registra el estado anterior, el nuevo, el autor y la fecha y hora.
- CA-03: Cuando el incidente ya está cerrado, entonces no se permite cambiarlo a "En atención" mediante la acción de inicio.

### HU-33 — Cerrar un incidente documentando el resultado
**Épica:** EP-08 — Gestión de incidentes
**Definición:** Como supervisor o responsable autorizado, quiero cerrar un incidente documentando el resultado, para dejar constancia de cómo terminó su atención.
**Criterios de aceptación:**
- CA-01: Dado un incidente en atención, cuando registro las acciones realizadas y un resultado de resolución, entonces puedo cambiarlo a "Cerrado".
- CA-02: Cuando intento cerrar sin resultado de resolución, entonces el sistema impide el cierre e indica qué información falta.
- CA-03: Cuando cierro un incidente con una desviación todavía activa, entonces se exige una justificación y la alerta térmica permanece activa.
- CA-04: Cuando el cierre se confirma, entonces se conserva el autor, la fecha y hora y el historial completo del incidente.

### HU-34 — Consultar incidentes y su historial
**Épica:** EP-08 — Gestión de incidentes
**Definición:** Como supervisor, quiero consultar incidentes y su historial, para revisar pendientes y reconstruir cómo se atendieron los problemas.
**Criterios de aceptación:**
- CA-01: Cuando filtro por punto, período, responsable o estado, entonces se muestran únicamente los incidentes coincidentes.
- CA-02: Cuando abro un incidente, entonces veo su descripción, autor, responsable, alertas vinculadas, acciones y cambios de estado en orden cronológico.
- CA-03: Cuando consulto un incidente cerrado o asociado a un dispositivo inactivo, entonces su información histórica continúa disponible.

---

# EP-09 — Exportación y auditoría

### HU-35 — Exportar las mediciones consultadas
**Épica:** EP-09 — Exportación y auditoría
**Definición:** Como supervisor, quiero exportar las mediciones consultadas, para analizarlas y compartir evidencia del comportamiento térmico.
**Criterios de aceptación:**
- CA-01: Cuando exporto una consulta con resultados, entonces se genera un archivo CSV con todos los registros que cumplen los filtros, aunque el listado esté paginado.
- CA-02: Cuando reviso el archivo, entonces incluye dispositivo, punto, temperatura, unidad, fecha y hora con zona horaria y límites aplicables cuando existan.
- CA-03: Cuando solicito la exportación, entonces el archivo contiene únicamente información dentro de mi ámbito de acceso.

### HU-36 — Consultar el registro de cambios (auditoría)
**Épica:** EP-09 — Exportación y auditoría
**Definición:** Como supervisor autorizado, quiero consultar un registro de cambios relevantes, para identificar quién modificó la configuración o la atención de incidentes.
**Criterios de aceptación:**
- CA-01: Cuando se modifican rangos, asociaciones de dispositivos, permisos o estados de incidentes, entonces se registra el autor, la fecha y hora, la operación y los valores anteriores y nuevos que correspondan.
- CA-02: Cuando filtro el registro por usuario, período o entidad afectada, entonces se muestran los eventos coincidentes en orden cronológico.
- CA-03: Cuando un usuario operativo consulta la auditoría, entonces no puede modificar ni eliminar sus registros mediante las funciones de la plataforma.