# # Stage 1: Build the application
# FROM eclipse-temurin:21-jdk-jammy AS builder

# WORKDIR /app
# COPY pom.xml .
# COPY .mvn .mvn
# COPY mvnw .
# # Make wrapper executable (though we will use the local maven cache if possible, it's good practice)
# RUN chmod +x ./mvnw || true

# # Copy source code
# COPY src src

# # Package the application (skip tests in Docker build, tests are run in CI)
# RUN ./mvnw clean package -DskipTests || mvn clean package -DskipTests

# # Stage 2: Create the runtime image
# FROM eclipse-temurin:21-jre-jammy

# WORKDIR /app

# # Create a non-root user
# RUN addgroup --system spring && adduser --system spring --ingroup spring

# # Create uploads directory and set permissions
# RUN mkdir -p /app/uploads && chown -R spring:spring /app/uploads

# # Switch to non-root user
# USER spring:spring

# # Copy the built WAR file from the builder stage
# # (Looking for .war or .jar depending on what Maven produces, usually .war because of packaging: war)
# COPY --from=builder --chown=spring:spring /app/target/tenant-complaint-management-system.war /app/app.war

# # Set environment variables with defaults
# ENV PORT=8080
# ENV DB_HOST=localhost
# ENV DB_PORT=3306
# ENV DB_NAME=tenant_complaints
# ENV DB_USERNAME=root
# ENV DB_PASSWORD=
# ENV UPLOAD_DIR=/app/uploads

# EXPOSE 8080

# # Health check
# HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
#   CMD wget -q -O - http://localhost:8080/actuator/health || exit 1

# # Run the application
# ENTRYPOINT ["java", "-jar", "/app/app.war"]


FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

COPY pom.xml .

RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /app/target/*.war app.war

RUN mkdir -p /app/uploads

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.war"]
