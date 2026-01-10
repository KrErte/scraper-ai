# Multi-stage build Java 21 rakenduse jaoks

# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Kopeeri pom.xml ja lae sõltuvused (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Kopeeri lähtekood ja ehita
COPY src ./src
RUN mvn package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Loo kasutaja turvalisuse jaoks
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Kopeeri JAR fail
COPY --from=build /app/target/*.jar app.jar

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
