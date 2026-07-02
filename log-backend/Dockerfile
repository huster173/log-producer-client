FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/*.jar app.jar

# logs/ directory will be mounted as a Docker volume
RUN mkdir -p /app/logs

EXPOSE 8081

# Shell form so LOG_FILE (OS env var) can be passed through as a JVM system
# property — Logback's ${VAR:-default} substitution only consults system
# properties, not OS environment variables.
ENTRYPOINT ["sh", "-c", "exec java -DLOG_FILE=${LOG_FILE:-app} -jar app.jar"]
