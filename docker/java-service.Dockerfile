# Shared image definition for every Spring Boot service.
#   docker build -f docker/java-service.Dockerfile --build-arg SERVICE=content-service .
#
# Compared with the old per-service Dockerfiles:
#  * only the poms + the one service's sources are copied, so editing one service no longer
#    invalidates the build cache of the other six;
#  * the Maven repository is a BuildKit cache mount, so dependencies download once per machine;
#  * the fat jar is split into Spring Boot layers (dependencies change rarely, app code often);
#  * the runtime runs as a non-root user with container-aware JVM memory settings.

ARG JAVA_VERSION=17

FROM maven:3.9-eclipse-temurin-${JAVA_VERSION} AS build
ARG SERVICE
WORKDIR /workspace

# Optional corporate/antivirus HTTPS-interception CAs (see README "Local Certificate Note").
COPY .local-certs/ /tmp/movento-local-certs/
RUN set -eux; for cert in /tmp/movento-local-certs/*.pem; do [ -f "$cert" ] || continue; \
      keytool -importcert -cacerts -storepass changeit -noprompt -alias "movento-local-$(basename "$cert" .pem)" -file "$cert"; \
    done; rm -rf /tmp/movento-local-certs

# The reactor needs every module pom, but only the target module's sources.
COPY pom.xml ./
COPY api-gateway/pom.xml api-gateway/
COPY service-registry/pom.xml service-registry/
COPY user-service/pom.xml user-service/
COPY content-service/pom.xml content-service/
COPY payment-service/pom.xml payment-service/
COPY streaming-service/pom.xml streaming-service/
COPY recommendation-service/pom.xml recommendation-service/
COPY ${SERVICE}/src ${SERVICE}/src

RUN --mount=type=cache,target=/root/.m2 \
    test -n "${SERVICE}" && \
    mvn -B -q -pl "${SERVICE}" -am -DskipTests package && \
    cp "${SERVICE}"/target/*.jar app.jar && \
    java -Djarmode=layertools -jar app.jar extract --destination extracted

FROM eclipse-temurin:${JAVA_VERSION}-jre-jammy AS runtime
ARG SERVICE
WORKDIR /app

COPY .local-certs/ /tmp/movento-local-certs/
RUN set -eux; \
    apt-get update; apt-get install -y --no-install-recommends curl; rm -rf /var/lib/apt/lists/*; \
    for cert in /tmp/movento-local-certs/*.pem; do [ -f "$cert" ] || continue; \
      keytool -importcert -cacerts -storepass changeit -noprompt -alias "movento-local-$(basename "$cert" .pem)" -file "$cert"; \
    done; rm -rf /tmp/movento-local-certs; \
    groupadd --system movento; useradd --system --gid movento --home-dir /app --shell /usr/sbin/nologin movento

# Least-frequently-changing layers first.
COPY --from=build --chown=movento:movento /workspace/extracted/dependencies/ ./
COPY --from=build --chown=movento:movento /workspace/extracted/spring-boot-loader/ ./
COPY --from=build --chown=movento:movento /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=movento:movento /workspace/extracted/application/ ./

USER movento
ENV SPRING_PROFILES_ACTIVE=prod \
    SERVICE_NAME=${SERVICE} \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
