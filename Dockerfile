# Build stage
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN ./gradlew --no-daemon dependencies
COPY src src

# Build the application
RUN ./gradlew build --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Create a non-root user to run the application
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy JAR from build stage
COPY --from=build --chown=spring:spring /app/build/libs/*.jar app.jar

# Configure health check
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]