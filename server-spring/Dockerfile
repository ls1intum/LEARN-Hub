# Multi-stage build for Spring Boot application

# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Production stage
FROM eclipse-temurin:21-jre-alpine

# Add labels for better image management
LABEL org.opencontainers.image.title="LEARN-Hub Server"
LABEL org.opencontainers.image.description="Spring Boot API server for LEARN-Hub application"
LABEL org.opencontainers.image.vendor="LEARN-Hub Project"

# Install curl for health checks
RUN apk add --no-cache curl

# Create app directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose application port
EXPOSE 5001

# Health check using curl
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:5001/hello || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
