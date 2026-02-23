# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent POM to correct location
COPY ../pom.xml ./pom.xml
COPY pom.xml ./api-gateway/pom.xml
COPY src ./api-gateway/src

# Build from the parent directory using reactor mode
RUN mvn package -DskipTests -pl api-gateway -am

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/api-gateway/target/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
