# ──────────────────────────────────────────────
# Etapa 1: Build con Maven
# ──────────────────────────────────────────────
FROM maven:3.9.11 AS builder
WORKDIR /app

COPY pom.xml .
RUN mvn -B -e -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -e -ntp -DskipTests package


# ──────────────────────────────────────────────
# Etapa 2: Imagen Final (Nginx + Java)
# ──────────────────────────────────────────────
FROM eclipse-temurin:21-jre

# Instalar Nginx + Redis
RUN apt-get update && apt-get install -y nginx redis-server && rm -rf /var/lib/apt/lists/*


WORKDIR /app

# Copiamos el JAR compilado
COPY --from=builder /app/target/*.jar app.jar

# Copiamos configuración nginx
COPY infra/nginx.conf /etc/nginx/nginx.conf

# Copiamos script de arranque
COPY infra/start.sh /start.sh
RUN chmod +x /start.sh

EXPOSE 80

CMD ["/start.sh"]
