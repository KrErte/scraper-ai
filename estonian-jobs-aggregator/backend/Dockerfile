# Multi-stage build Java 21 rakenduse jaoks (Gradle)

# Build stage
FROM gradle:8.5-jdk21 AS build
WORKDIR /app

# Kopeeri Gradle failid ja lae sõltuvused (cache layer)
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon || true

# Kopeeri lähtekood ja ehita
COPY src ./src
RUN gradle bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Loo kasutaja turvalisuse jaoks
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Kopeeri JAR fail
COPY --from=build /app/build/libs/*.jar app.jar

# Muuda omanik
RUN chown -R appuser:appgroup /app

# Vaheta kasutajale
USER appuser

# Ava port
EXPOSE 8080

# Tervisekontrolli jaoks
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Käivita rakendus
ENTRYPOINT ["java", "-jar", "app.jar"]
