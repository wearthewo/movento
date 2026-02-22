# Build stage
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app

# Copy only the POM file first to leverage Docker cache
COPY pom.xml .

# Download all dependencies
RUN mvn dependency:go-offline -B

# Copy the rest of the source code
COPY src ./src

# Build the application
RUN mvn package -DskipTests

# Run stage
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
