FROM gradle:8.10.0-jdk17 AS builder
WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . .
RUN ./gradlew :notification:shadowJar --no-daemon
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /home/gradle/project/notifications/build/libs/*.jar app.jar
EXPOSE 8082
CMD ["java", "-jar", "app.jar"]
