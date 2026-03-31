# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (layer cache optimization)
RUN chmod +x mvnw && ./mvnw dependency:resolve

# Copy source code
COPY src ./src

# Build application
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose port (Spring Boot default)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD java -cp app.jar org.springframework.boot.loader.JarLauncher || exit 1

# Run application with environment variables for Supabase
ENTRYPOINT ["java", "-jar", "app.jar"]
