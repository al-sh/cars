# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Кэширование зависимостей — скачиваем отдельно от исходников.
# Docker переиспользует этот слой, пока pom.xml не изменился.
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Сборка JAR
COPY src src
RUN mvn package -DskipTests -B -q

# Runtime stage — только JRE, без JDK и Maven
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root пользователь для безопасности
RUN addgroup -g 1000 app && adduser -u 1000 -G app -D app
USER app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
