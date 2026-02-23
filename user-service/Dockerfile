# -------- BUILD STAGE --------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy entire project
COPY . .

# Build only api-gateway module and required parents
RUN mvn clean package -pl user-service -am -DskipTests

# -------- RUN STAGE --------
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/user-service/target/*.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]
