# -------- BUILD STAGE --------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Trust optional local HTTPS-interception CAs during Maven downloads.
COPY .local-certs/ /tmp/movento-local-certs/
RUN set -eux; for cert in /tmp/movento-local-certs/*.pem; do [ -f "$cert" ] || continue; keytool -importcert -cacerts -storepass changeit -noprompt -alias "movento-local-$(basename "$cert" .pem)" -file "$cert"; done; rm -rf /tmp/movento-local-certs

# Copy entire project
COPY . .

# Build only api-gateway module and required parents
RUN mvn clean package -pl content-service -am -DskipTests

# -------- RUN STAGE --------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Trust optional local HTTPS-interception CAs for runtime API calls.
COPY .local-certs/ /tmp/movento-local-certs/
RUN set -eux; for cert in /tmp/movento-local-certs/*.pem; do [ -f "$cert" ] || continue; keytool -importcert -cacerts -storepass changeit -noprompt -alias "movento-local-$(basename "$cert" .pem)" -file "$cert"; done; rm -rf /tmp/movento-local-certs

COPY --from=build /app/content-service/target/*.jar app.jar

EXPOSE 8085

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]
