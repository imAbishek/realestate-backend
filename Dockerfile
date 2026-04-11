# ============================================================
#  Dockerfile  —  Multi-stage build
#
#  Stage 1: Build the JAR using Maven
#  Stage 2: Run the JAR using a slim JRE image
#
#  Build:  docker build -t realestate-backend .
#  Run:    docker run -p 8080:8080 --env-file .env realestate-backend
# ============================================================

# ── Stage 1: Build ──────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom first (layer cache — only re-downloads
# dependencies when pom.xml changes, not on every code change)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Now copy source and build
COPY src ./src
RUN ./mvnw package -DskipTests -B

# ── Stage 2: Run ────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Security: don't run as root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy the built JAR from stage 1
COPY --from=builder /app/target/*.jar app.jar

# Create log directory
RUN mkdir -p /var/log/realestate

EXPOSE 8080

# Pass SPRING_PROFILES_ACTIVE=prod via env var when running in production
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
