# Stage 1: Cache Gradle dependencies
FROM gradle:8.10.2-jdk21 AS cache
WORKDIR /home/gradle/app

# Copy Gradle build files (but NOT the full source yet)
COPY build.gradle.kts gradle.properties settings.gradle.kts ./
COPY gradle ./gradle

# Download dependencies without building the project
RUN gradle dependencies --no-daemon || return 0

# Stage 2: Build the application
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /home/gradle/src

# Copy cached Gradle data from previous stage
COPY --from=cache /home/gradle/.gradle /home/gradle/.gradle

# Copy all source code
COPY . .

# Build the fat JAR (this works with either Shadow or Ktor plugin)
RUN gradle shadowJar --no-daemon

# Stage 3: Runtime image
FROM openjdk:21-jdk-slim AS runtime
WORKDIR /app
EXPOSE 8080

# Copy built JAR
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

# Run your app
ENTRYPOINT ["java", "-jar", "app.jar"]
