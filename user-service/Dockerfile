# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent POM and service files
COPY ../pom.xml ../
COPY pom.xml ./user-service/pom.xml
COPY src ./user-service/src

# Build service with proper parent resolution
WORKDIR /app/user-service
RUN mvn package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy JAR file from build stage
COPY --from=build /app/user-service/target/*.jar app.jar

# Expose port app runs on
EXPOSE 8081

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]