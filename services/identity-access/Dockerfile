# ==========================================
# Etapa 1: Build y compilación (Builder)
# ==========================================
FROM --platform=$BUILDPLATFORM maven:3.9.11-eclipse-temurin-17 AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn -B -ntp dependency:resolve dependency:resolve-plugins

COPY src src
RUN mvn -B -ntp clean package -DskipTests -Djacoco.skip=true

# ==========================================
# Etapa 2: Entorno de ejecución ligero (Runner)
# ==========================================
FROM eclipse-temurin:17-jre-noble AS runner

WORKDIR /app

RUN groupadd --system --gid 10001 appgroup && \
    useradd --system --uid 10001 --gid appgroup --no-create-home appuser

COPY --from=builder --chown=appuser:appgroup /build/target/identity-access-*.jar app.jar

USER appuser

EXPOSE 8083

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
