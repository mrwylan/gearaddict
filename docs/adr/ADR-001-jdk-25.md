# ADR-001: JDK 25 as Runtime Platform

## Status
Accepted

## Context

The project requires a stable, long-term Java runtime. JDK 25 was released on September 16, 2025 and is the current Long-Term Support (LTS) release, superseding JDK 21 LTS. It is the recommended JDK for production deployments of Spring Boot 4.x and is supported through at least 2030 under standard LTS policies. JDK 26 (non-LTS) was released in March 2026 but offers no additional benefit for this project.

Spring Boot 4.0.5 requires Java 17 as an absolute minimum and officially supports Java 17 through 26. The effective minimum for the full stack is Java 21, imposed by Vaadin 25.x. JDK 25 satisfies all constraints.

## Decision

Use **JDK 25** as the Java runtime for the GearAddict backend and build toolchain.

## Consequences

- Access to all language and JVM features through Java 25 (virtual threads via Project Loom GA, pattern matching, records, sealed classes, etc.)
- Compatible with Spring Boot 4.0.5, Vaadin 25.1.1, jOOQ 3.21.1, and Flyway 12.3.0
- LTS support guarantees long-term security patches — no runtime upgrade required for the foreseeable project lifetime
- Build infrastructure (CI, Docker base images) must use a JDK 25 distribution (e.g., Eclipse Temurin 25)
