# Student API — Nooruzbek Toktobaev
# Multi-stage build: stage 1 builds the JAR with Gradle (JDK), stage 2 runs it on a small JRE image.

# ---------- Stage 1: build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Gradle wrapper and build scripts first: this layer (and downloaded dependencies)
# is cached and reused while only the source code changes
COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q > /dev/null

COPY src ./src
# Tests are skipped here because Jenkins runs them in a separate Test stage before Docker Build
RUN ./gradlew clean bootJar -x test --no-daemon

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine

ARG APP_VERSION=1.1.0
LABEL org.opencontainers.image.title="toktobaev-nooruzbek-student-api" \
      org.opencontainers.image.description="Spring Boot CRUD Student API" \
      org.opencontainers.image.authors="Nooruzbek Toktobaev" \
      org.opencontainers.image.version="${APP_VERSION}" \
      student="Nooruzbek Toktobaev"

WORKDIR /app
# Run the application as an unprivileged user, not root
RUN addgroup -S student && adduser -S -G student student
COPY --from=build /app/build/libs/app.jar /app/app.jar
USER student

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
