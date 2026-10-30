# ==========================================
# Etapa 1: Build y compilación (Builder)
# ==========================================
FROM --platform=$BUILDPLATFORM maven:3.9.11-eclipse-temurin-17 AS builder

WORKDIR /build

# Copiar descriptores de dependencias y configuración de calidad
COPY pom.xml .
COPY config config

# Descarga de dependencias en capa cacheada
RUN mvn -B -ntp dependency:resolve

# Copiar código fuente y construir el paquete ejecutable
COPY src src
RUN mvn -B -ntp clean package -DskipTests

# ==========================================
# Etapa 2: Entorno de ejecución ligero (Runner)
# ==========================================
FROM eclipse-temurin:17-jre-noble AS runner

WORKDIR /app

# Crear usuario y grupo de sistema sin privilegios (Seguridad: No-root user)
RUN groupadd --system --gid 10001 appgroup && \
    useradd --system --uid 10001 --gid appgroup --no-create-home appuser

# Copiar el artefacto empaquetado con permisos exclusivos para appuser
COPY --from=builder --chown=appuser:appgroup /build/target/monitoring-management-*.jar app.jar

# Configurar el usuario no root
USER appuser

EXPOSE 8080

# Parámetros optimizados para ejecución en contenedores
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
