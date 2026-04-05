# ADR-008: OCI Container Image as Release Artifact

## Status
Accepted

## Context

The application must be deployable in a reproducible, environment-independent way. Packaging as an OCI container image is the standard approach for Java web applications that need to run consistently across development, staging, and production environments.

Spring Boot 4.x provides first-class support for building OCI images via the `spring-boot:build-image` Maven goal, which uses Cloud Native Buildpacks (CNB) and produces a layered image without requiring a hand-written Dockerfile. This is the preferred approach as it handles JVM memory tuning, layer optimization, and base image selection automatically.

Alternatives considered:
- **Fat JAR only** — not environment-independent; requires the host to have a matching JDK installed
- **Hand-written Dockerfile** — more control but duplicates concerns already handled by CNB (layering, JVM flags, non-root user)
- **Native image (GraalVM)** — reduces startup time but Vaadin Flow does not yet support GraalVM native compilation

## Decision

Produce an **OCI-compliant container image** as the release artifact using Spring Boot's `build-image` goal (Cloud Native Buildpacks). The image is published to a container registry from the GitHub Actions release workflow (see ADR-007).

## Consequences

- `./mvnw spring-boot:build-image` produces a production-ready image locally and in CI
- Base image is managed by the Paketo Buildpack for Java; the JDK 25 Temurin buildpack must be configured
- Image name and tag are configured in `pom.xml` under `<plugin><configuration><image>`
- The image is pushed to the configured container registry (e.g., GitHub Container Registry `ghcr.io`) on tagged releases
- The application does not bundle PostgreSQL — the container expects a `DATABASE_URL` (or equivalent Spring datasource properties) injected at runtime via environment variables
- Flyway migrations run automatically at application startup via Spring Boot auto-configuration
- No hand-written `Dockerfile` is needed or should be added
