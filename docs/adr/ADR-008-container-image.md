# ADR-008: OCI Container Image as Release Artifact

## Status
Accepted

## Context

The application must be deployable in a reproducible, environment-independent way across heterogeneous hardware — notably both `linux/amd64` (typical cloud VMs, x86 developer machines) and `linux/arm64` (Apple Silicon, Ampere/Graviton cloud instances). Packaging as an OCI container image is the standard approach for Java web applications that need to run consistently across development, staging, and production environments.

Spring Boot 4.x provides first-class support for building OCI images via the `spring-boot:build-image` Maven goal, which uses Cloud Native Buildpacks (CNB) and produces a layered image without requiring a hand-written Dockerfile. CNB handles JVM memory tuning, layer optimization, and base image selection automatically.

However, `spring-boot:build-image` has a **significant limitation for multi-platform builds**: the Paketo builder produces a single image matching the host architecture of the build environment, and does not emit a multi-arch manifest list. Cross-compiling to a different architecture requires running the builder under emulation on that target platform and then combining the resulting images manually — CNB does not natively orchestrate this.

Alternatives considered:
- **Fat JAR only** — not environment-independent; requires the host to have a matching JDK installed
- **`spring-boot:build-image` alone** — excellent for single-arch, but cannot directly produce a `linux/amd64` + `linux/arm64` manifest list
- **Hand-written Dockerfile + `docker buildx`** — buildx natively supports multi-platform builds via QEMU emulation or native multi-node builders, and emits a proper OCI manifest list; the Dockerfile duplicates some CNB concerns (layering, JVM flags, non-root user) but is the standard tool for multi-arch publishing
- **Native image (GraalVM)** — reduces startup time but Vaadin Flow does not yet support GraalVM native compilation

## Decision

Produce an **OCI-compliant, multi-platform container image** (`linux/amd64` and `linux/arm64`) as the release artifact, built with **Docker Buildx** from a hand-written `Dockerfile`. The image is published as a single multi-arch manifest list to the configured container registry from the GitHub Actions release workflow (see ADR-007).

Buildx is chosen over `spring-boot:build-image` specifically because CNB cannot emit a multi-arch manifest list in a single invocation, and multi-platform support is a hard requirement. The JVM-tuning and layering ergonomics of CNB are replicated in the Dockerfile using the Spring Boot layered-jar support (`java -Djarmode=tools extract --layers`).

## Consequences

- A `Dockerfile` at the repository root defines the image; it consumes the layered jar produced by `./mvnw package`
- `docker buildx build --platform linux/amd64,linux/arm64 --push …` produces and publishes the multi-arch image in CI
- The GitHub Actions release workflow uses `docker/setup-qemu-action` and `docker/setup-buildx-action` to enable cross-platform builds on standard `ubuntu-latest` runners
- Local developers can build a single-arch image for their host with `docker buildx build --load .`
- Base image is a JDK 25 Temurin image (multi-arch); a non-root user is configured explicitly in the Dockerfile
- Image name and tag are controlled by the workflow (e.g., GitHub Container Registry `ghcr.io`) rather than `pom.xml`
- The application does not bundle PostgreSQL — the container expects a `DATABASE_URL` (or equivalent Spring datasource properties) injected at runtime via environment variables
- Flyway migrations run automatically at application startup via Spring Boot auto-configuration
- `spring-boot:build-image` is no longer used for release artifacts; it may still be used ad-hoc for local experimentation but is not part of the release path
