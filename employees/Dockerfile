# Stage 1: Build the fat jar using Gradle
FROM gradle:8.10.0-jdk17 AS builder
WORKDIR /home/gradle/project
# Копируем весь проект; сборка будет осуществляться из корневой директории
COPY --chown=gradle:gradle . .
# Собираем fat-jar для модуля employees; убедитесь, что задача :employees:shadowJar настроена
RUN ./gradlew :employees:shadowJar --no-daemon

# Stage 2: Create runtime image
FROM openjdk:17-jdk-slim
WORKDIR /app
# Копируем jar-файл из stage builder
COPY --from=builder /home/gradle/project/employees/build/libs/*.jar app.jar
EXPOSE 8081
CMD ["java", "-jar", "app.jar"]
