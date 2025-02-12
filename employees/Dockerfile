FROM gradle:8.12.1-jdk21-corretto AS builder
WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . .
RUN ./gradlew :employees:shadowJar --no-daemon

FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=builder /home/gradle/project/employees/build/libs/*.jar app.jar
EXPOSE 8081
CMD ["java", "-jar", "app.jar"]
