# language: es
# Automatizado: probes y PodDisruptionBudget (Kubernetes), alerta ColdChainServiceDown
# y prueba de recuperación descrita en 4.1 / runbook RB-01.
@NFR @HU39 @disponibilidad @automatizado
Característica: Disponibilidad del servicio
  Como supervisor quiero que la plataforma siga disponible ante fallas de un componente
  para no perder visibilidad de la cadena de frío.

  Escenario: Recuperación automática de un pod eliminado
    Dado el ambiente "coldchain-prod" con todos los servicios listos
    Cuando se elimina el pod de "telemetry-ingestion"
    Entonces Kubernetes crea un pod de reemplazo
    Y el servicio vuelve a estar listo en menos de 90 segundos

  Escenario: Despliegue sin interrupción
    Dado 2 réplicas del frontend atendiendo solicitudes
    Cuando se despliega una nueva versión con RollingUpdate (maxUnavailable 0)
    Entonces ninguna solicitud de monitoreo responde con error 5xx

  Escenario: Alerta ante un servicio caído
    Cuando un servicio deja de responder durante 1 minuto
    Entonces Prometheus dispara la alerta "ColdChainServiceDown"
    Y Alertmanager notifica al receptor "guardia"
