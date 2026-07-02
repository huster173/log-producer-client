FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY build/libs/*.jar app.jar

# logs/ directory will be mounted as a Docker volume
RUN mkdir -p /app/logs

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
