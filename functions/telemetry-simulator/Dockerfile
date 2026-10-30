# ====================================================================
# Simulador de telemetría - ejecución local en contenedor (solo demo)
# En la nube este componente se despliega como Azure Function (Timer
# Trigger). Este contenedor reutiliza simulator.py (solo stdlib) en un
# bucle, sin Azure Functions Core Tools ni Azurite.
# ====================================================================
FROM python:3.12-alpine

WORKDIR /app

# Usuario sin privilegios (mismo criterio que los servicios Java)
RUN addgroup -S -g 10001 appgroup && \
    adduser -S -u 10001 -G appgroup appuser

# simulator.py no tiene dependencias externas; function_app.py no se usa aquí
COPY --chown=appuser:appgroup simulator.py .

USER appuser

ENV PYTHONUNBUFFERED=1

# Una medición por iteración. Un fallo se registra y NO se reintenta
# (la ingesta no deduplica); la siguiente iteración es una medición nueva.
CMD ["sh", "-c", "while true; do python simulator.py || echo 'simulator: ejecucion fallida, sin reintento automatico'; sleep \"${SIMULATOR_INTERVAL_SECONDS:-60}\"; done"]

