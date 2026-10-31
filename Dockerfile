# ==============================================================================
# Production Dockerfile for Venue Resource Booking System
# Base Image: Eclipse Temurin OpenJDK 21 JRE (Lightweight, Alpine/Ubuntu minimal)
# ==============================================================================
FROM eclipse-temurin:21-jre

LABEL maintainer="Rishi Phadale <rishiphadale@gmail.com>" \
      application="venue-booking-system" \
      version="1.0.0-rc.1" \
      description="Venue Resource Booking System Spring Boot Web Application"

# Set application working directory
WORKDIR /app

# Configure default container environment variables
ENV SERVER_PORT=8081 \
    SPRING_PROFILES_ACTIVE=dev \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Create directories for persistent data (H2 database) and logs
RUN mkdir -p /app/data /app/logs

# Define volume mount point for persistent storage across container restarts
VOLUME ["/app/data"]

# Copy compiled WAR package into container
COPY target/venue-booking.war app.war

# Expose default HTTP application port
EXPOSE 8081

# Native Docker health check querying the /health JSON endpoint
# - interval: every 15s to detect downtime quickly
# - timeout: 5s per probe
# - start-period: 30s to allow JVM warmup and Spring Boot bootstrap
# - retries: 3 consecutive failures mark container as UNHEALTHY
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:${SERVER_PORT}/health || exit 1

# Execute Java with exec to ensure it receives SIGTERM/SIGINT signals directly (PID 1)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.war"]
