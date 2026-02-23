# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent POM to correct location
COPY ../pom.xml ./pom.xml
COPY pom.xml ./recommendation-service/pom.xml
COPY src ./recommendation-service/src

# Build from the parent directory using reactor mode
RUN mvn package -DskipTests -pl recommendation-service -am

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/recommendation-service/target/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8084

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
