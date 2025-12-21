# Multi-stage build for a smaller final image
# Stage 1: Build the application
FROM maven:3.9-amazoncorretto-17 AS build
WORKDIR /app
COPY pom.xml .
# Copy source code
COPY src ./src
# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar
# Expose the port your app runs on
EXPOSE 8081
# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]