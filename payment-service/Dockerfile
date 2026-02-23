# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the entire project structure first
COPY . .

# Build the specific service
RUN mvn package -DskipTests -pl payment-service -am

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/payment-service/target/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8083

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
