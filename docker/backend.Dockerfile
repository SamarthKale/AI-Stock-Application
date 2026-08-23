# syntax=docker/dockerfile:1
# Phase 6 — multi-stage build. Base images verified multi-arch (linux/arm64 included) on Docker
# Hub before this was written (CLAUDE.md's Phase 6 plan §7 step 6) — targets the OCI Ampere A1
# VM via `docker buildx build --platform linux/arm64`, same Dockerfile also builds linux/amd64
# locally with no changes.
#
# Build context is the repo root (see docker-compose.yml's `context: ..`), not backend/, so this
# file can COPY only what it needs rather than the whole backend/ tree (build/, .gradle/ caches).
#
# Build stage: the official `gradle` image, pinned to the exact same version the project's
# wrapper pins (backend/gradle/wrapper/gradle-wrapper.properties — currently 9.7.1, JDK 21),
# confirmed multi-arch including linux/arm64/v8 on Docker Hub. This is the fix for
# `java.net.ConnectException: Connection refused` to services.gradle.org from inside the Docker
# build network: that's an active refusal, not a slow connection, so no wrapper
# networkTimeout/retries tuning (already applied — see gradle-wrapper.properties) could fix it.
# Using a base image with Gradle already installed means the build stage never needs to reach
# services.gradle.org at all. The project's Gradle Wrapper is NOT removed — gradlew/gradle/ are
# still copied into the image below and remain the source of truth for every other environment
# (host builds, CI, local dev outside Docker); only this Docker build stage invokes the
# pre-installed `gradle` binary directly, and only because it is the exact same 9.7.1 version the
# wrapper pins. If the project's Gradle version is ever bumped, update both this FROM tag and
# gradle-wrapper.properties together, or the two builds will silently drift apart.
FROM gradle:9.7.1-jdk21 AS build
WORKDIR /app
COPY backend/gradlew backend/gradlew.bat ./
COPY backend/gradle ./gradle
RUN chmod +x gradlew
COPY backend/build.gradle.kts backend/settings.gradle.kts ./
# BuildKit cache mount (needs the `# syntax` line above + buildx) for GRADLE_USER_HOME (the
# official gradle image runs as root, HOME=/root, so this is still /root/.gradle — confirmed by
# running the image directly). Persists resolved dependency jars across separate
# `docker build`/`docker buildx build` invocations — including ones that don't hit the regular
# image layer cache (a fresh builder, --no-cache, a different host).
RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    gradle --no-daemon dependencies || true
COPY backend/src ./src
RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    gradle --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Non-root — defense-in-depth on a single shared VM where every service lives in one Docker
# network (see CLAUDE.md's Phase 6 architecture: this is the entire production environment).
RUN useradd --system --uid 10001 --create-home appuser
COPY --from=build /app/build/libs/*.jar app.jar
USER appuser
EXPOSE 8080
# SPRING_PROFILES_ACTIVE=docker is set in docker-compose.yml — switches logback-spring.xml to
# JSON output (see backend/src/main/resources/logback-spring.xml).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70.0", "-jar", "app.jar"]
