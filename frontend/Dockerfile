# ==========================================
# Etapa 1: Build de la SPA con Vite (Builder)
# ==========================================
FROM --platform=$BUILDPLATFORM node:22-alpine AS builder

WORKDIR /app

# Copiar descriptores de dependencias npm para aprovechar la caché
COPY package.json package-lock.json ./

# Instalación limpia de dependencias
RUN npm ci

# Copiar el código fuente de la aplicación
COPY . .

# Variables de entorno públicas para la compilación con Vite
ENV VITE_REGISTRY_BASE_URL=/registry
ENV VITE_TELEMETRY_BASE_URL=/telemetry
ENV VITE_ALERTS_BASE_URL=/alerts

# Generar el paquete de producción en /app/dist
RUN npm run build

# ==========================================
# Etapa 2: Servidor web de producción (Runner)
# ==========================================
# Imagen sin privilegios: Nginx corre como el usuario nginx (UID 101) y escucha en 8080.
FROM nginxinc/nginx-unprivileged:1.27-alpine-slim AS runner

# Plantilla de Nginx con reverse proxy; envsubst la renderiza al arrancar
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

# Upstreams por defecto (DNS de Compose); sobrescribir en Azure Container Apps
ENV REGISTRY_UPSTREAM=http://monitoring-registry:8080 \
    TELEMETRY_UPSTREAM=http://telemetry-ingestion:8081 \
    ALERTS_UPSTREAM=http://alert-management:8082

# Copiar los archivos estáticos generados por el builder
COPY --from=builder /app/dist /usr/share/nginx/html

USER nginx

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]

