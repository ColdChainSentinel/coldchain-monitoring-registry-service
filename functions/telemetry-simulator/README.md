# Simulador serverless de telemetría — apartado 3.5

Responsable: Joaquín Carbajal. Azure Functions v4, Python 3.12, modelo Python v2.
Función de apoyo a desarrollo y pruebas; **no es un sensor real ni un notificador**.
El Timer Trigger emite una medición por ejecución hacia el contrato existente
`POST /api/measurements`. Telemetry persiste y solicita evaluación a Alerts;
la función no consulta bases, no decide umbrales y no crea alertas directamente.

## Prueba inmediata sin Azure ni dependencias

Desde esta carpeta:

```sh
python3 -m unittest discover -s tests -v
MONITORING_POINT_ID=11111111-1111-4111-8111-111111111111 python3 simulator.py
```

La segunda instrucción muestra el payload sin enviarlo (`SIMULATOR_DRY_RUN=true`
por defecto). Ese UUID es solo ilustrativo para dry-run. La CLI usa variables
del proceso; `local.settings.json` solo es cargado por Core Tools.

## Ejecutar el temporizador local

Requiere Python 3.12, Azure Functions Core Tools v4 y Azurite ejecutándose para
el almacenamiento del temporizador. Desde esta carpeta:

```sh
python3.12 -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
cp local.settings.example.json local.settings.json
func start
```

Antes de `func start`, editar el UUID en `local.settings.json`. Enviar de verdad
requiere Registry y Telemetry funcionales, punto existente, conectividad y regla
explícita en Alerts. Cambiar `SIMULATOR_DRY_RUN` a `false` y elegir temperatura.
El negocio de Telemetry/Alerts aún no está implementado en esta rama: actualmente
el envío responde 404. Dry-run y tests no acreditan integración completa.

| Variable | Uso |
|---|---|
| `SIMULATOR_SCHEDULE` | NCRONTAB de seis campos; `0 * * * * *` = cada minuto UTC |
| `MONITORING_POINT_ID` | UUID real creado en Registry; obligatorio incluso en dry-run |
| `SIMULATED_TEMPERATURE` | Decimal Celsius finito; default 5; sin umbral implícito |
| `TELEMETRY_BASE_URL` | Origen HTTP(S), default `http://localhost:8081` |
| `HTTP_TIMEOUT_SECONDS` | Timeout de transporte, >0 y <=30; default 5 |
| `SIMULATOR_DRY_RUN` | `true` valida y registra sin HTTP; `false` envía |
| `AzureWebJobsStorage` | Almacenamiento del host; Azurite local, cuenta real en Azure |
| `FUNCTIONS_WORKER_RUNTIME` | `python` |

No se ejecuta al arrancar el host. `use_monitor=True` conserva seguimiento del
calendario usando almacenamiento; **no garantiza entrega exactamente una vez**.
Si llega tarde, emite una lectura con hora actual sin inventar lecturas históricas.
No hay política de retry de la función ni reintentos HTTP: el contrato de ingesta
no deduplica. Tras timeout puede existir una medición persistida; comprobarla antes
de reenviar. Las siguientes ejecuciones programadas son mediciones nuevas.

Un 201 con `COMPLETED` indica evaluación terminada, incluso sin alerta; `PENDING`
indica evaluación pendiente y `RULE_NOT_CONFIGURED`, regla ausente. No se convierten
en supuesta alerta ni se reenvía la ingesta para resolverlos. Redirecciones rechazadas.

## Integración y publicación con el equipo

IaC y despliegue deben proporcionar Function App Linux/Python 3.12/runtime v4,
almacenamiento del host y settings anteriores. Configurar `FUNCTIONS_EXTENSION_VERSION=~4`
en Azure. La URL de Telemetry debe ser accesible desde la Function App; `localhost`
no apunta al microservicio en cloud. No necesita HTTP trigger ni puerto público.
Definir con los responsables de 3.3/3.4 el plan, región, red privada/TLS y acceso a
Telemetry; las APIs actuales no implementan autenticación. Reservar este simulador
para desarrollo/staging y mantenerlo deshabilitado en producción.

Publicación manual, una vez que exista el recurso y su configuración:

```sh
func azure functionapp publish NOMBRE_REAL_DE_LA_FUNCTION_APP --build remote
```

Este comando es una instrucción para el responsable de despliegue; no se ha ejecutado.
No se crean recursos, suscripciones ni credenciales desde esta entrega.
El workflow `ci-serverless.yml` instala el SDK, ejecuta tests y verifica el registro
de la función en Python 3.12; no despliega.

## Demostración integrada pendiente

1. Crear punto real y configurar su regla en Alerts (responsable funcional).
2. En desarrollo, usar una temperatura dentro del rango: comprobar medición y ausencia
   de alerta. Repetir con temperatura fuera del rango: comprobar alerta `OPEN`.
3. Probar falta de regla: distinguir `RULE_NOT_CONFIGURED`. Interrumpir Alerts:
   comprobar `PENDING`; interrumpir Telemetry: comprobar fallo sin reenvío.
4. Guardar commit, settings sin secretos, hora UTC, ID de medición, logs y evidencia
   de consulta de alerta. Medir latencia real; no atribuir cumplimiento de <60s al
   temporizador de un minuto ni afirmar que generar alerta equivale a notificar.
5. Detener/deshabilitar el simulador tras la demo para evitar datos de prueba continuos.

Referencia oficial: [Timer Trigger](https://learn.microsoft.com/en-us/azure/azure-functions/functions-bindings-timer?tabs=python-v2),
[Python](https://learn.microsoft.com/en-us/azure/azure-functions/functions-reference-python).
