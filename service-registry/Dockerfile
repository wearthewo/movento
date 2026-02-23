# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent POM to correct location
COPY ../pom.xml ../
COPY pom.xml ./service-registry/pom.xml
COPY src ./service-registry/src

# Build from the parent directory using reactor mode
WORKDIR /app/service-registry
RUN mvn package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/service-registry/target/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8761

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
