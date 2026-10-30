# ==========================================
# Etapa 1: Build de la SPA con Vite (Builder)
# ==========================================
FROM node:22-alpine AS builder

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
FROM nginx:1.27-alpine-slim AS runner

# Copiar configuración optimizada de Nginx con reverse proxy
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copiar los archivos estáticos generados por el builder
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

