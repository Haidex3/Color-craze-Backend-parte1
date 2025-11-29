# ──────────────────────────────────────────────
# Etapa 1: Build con Maven
# ──────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21-slim AS builder
WORKDIR /app

# Copiamos pom.xml y descargamos dependencias en cache
COPY pom.xml .
RUN mvn -B -e -ntp dependency:go-offline

# Copiar el código fuente
COPY src ./src

# Compilar y empaquetar
RUN mvn -B -e -ntp clean package -DskipTests

# ──────────────────────────────────────────────
# Etapa 2: Imagen final liviana JRE
# ──────────────────────────────────────────────
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiar JAR generado de la etapa anterior
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
