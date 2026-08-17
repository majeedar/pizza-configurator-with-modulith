# Tests (Testcontainers-based, need a Docker daemon) run in CI as a separate
# step before this build; packaging here skips them rather than trying to run
# Docker-in-Docker inside the build stage.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B clean package -DskipTests

### The Ubuntu-based `eclipse-temurin:21-jre` tag bundles Canonical's Pebble
# binary (unrelated to this app — used by Canonical's own image tooling),
# which as of this writing ships 8 unfixed HIGH Go-stdlib CVEs baked into a
# non-apt binary (`dpkg -S` finds no owning package, so `apt-get upgrade`
# can't touch it either) — found via the Trivy scan wired into
# container-build.yml. The `-alpine` variant is a from-scratch Alpine base
# with no Pebble at all, is smaller, and is an officially supported Temurin
# tag (not a hand-rolled minimal image), so it's the fix here rather than
# something to merely document as an accepted risk.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# curl backs the HEALTHCHECK below (compose.yaml's backend service and the
# production nginx upstream both depend on container-level health, agent.md
# §25.2 "real health checks rather than fixed sleep commands"). `apk upgrade`
# pulls current patched versions of whatever the base image's own Alpine
# packages are, rather than freezing whatever was current when the base
# image itself was last published (caught 3 HIGH CVEs — libexpat, p11-kit —
# via the Trivy scan wired into container-build.yml).
RUN apk update && apk upgrade --no-cache && apk add --no-cache curl
COPY --from=build /workspace/target/*.jar app.jar
USER 10001
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
