# Multi-stage build for Spring Boot Backend
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build
COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src src

# Binary builds uploaded from Windows can contain CRLF despite the repository storing LF.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw clean package -DskipTests

# Runtime stage with minimal non-root image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S -g 10001 appgroup && adduser -S -u 10001 appuser -G appgroup

COPY --from=builder /build/target/*.jar app.jar
RUN chown -R appuser:appgroup /app

# A numeric UID lets Kubernetes verify runAsNonRoot; OpenShift can still assign its own UID.
USER 10001:0

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
