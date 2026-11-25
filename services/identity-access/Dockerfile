# ==========================================
# Etapa 1: Build y compilación (Builder)
# ==========================================
FROM --platform=$BUILDPLATFORM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn -B -ntp dependency:resolve

COPY src src
RUN mvn -B -ntp clean package -DskipTests

# ==========================================
# Etapa 2: Entorno de ejecución ligero (Runner)
# ==========================================
FROM eclipse-temurin:17-jre-alpine AS runner

WORKDIR /app

RUN addgroup -S -g 10001 appgroup && \
    adduser -S -u 10001 -G appgroup appuser

COPY --from=builder --chown=appuser:appgroup /build/target/identity-access-*.jar app.jar

USER appuser

EXPOSE 8083

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
