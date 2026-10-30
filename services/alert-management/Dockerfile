# ==========================================
# Etapa 1: Build y compilación (Builder)
# ==========================================
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Copiar descriptores de dependencias para aprovechar caché de Docker
COPY pom.xml .

# Descarga de dependencias en capa cacheada
RUN mvn -B -ntp dependency:resolve

# Copiar código fuente y construir el paquete ejecutable
COPY src src
RUN mvn -B -ntp clean package -DskipTests

# ==========================================
# Etapa 2: Entorno de ejecución ligero (Runner)
# ==========================================
FROM eclipse-temurin:17-jre-alpine AS runner

WORKDIR /app

# Crear usuario y grupo de sistema sin privilegios (Seguridad: No-root user)
RUN addgroup -S -g 10001 appgroup && \
    adduser -S -u 10001 -G appgroup appuser

# Copiar el artefacto ejecutable con permisos restringidos
COPY --from=builder --chown=appuser:appgroup /build/target/alert-management-*.jar app.jar

# Configurar el usuario no root
USER appuser

EXPOSE 8082

# Parámetros optimizados para ejecución en contenedores
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

