# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent POM to correct location
COPY ../pom.xml ./pom.xml
COPY pom.xml ./streaming-service/pom.xml
COPY src ./streaming-service/src

# Build from the parent directory using reactor mode
RUN mvn package -DskipTests -pl streaming-service -am

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/streaming-service/target/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8083

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
