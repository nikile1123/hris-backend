# Stage 1: Build the fat jar using Gradle
FROM gradle:8.12.1-jdk21-corretto AS builder
WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . .
RUN ./gradlew :reviews:shadowJar --no-daemon

# Stage 2: Create a runtime image
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=builder /home/gradle/project/reviews/build/libs/*.jar app.jar
EXPOSE 8083
CMD ["java", "-jar", "app.jar"]
