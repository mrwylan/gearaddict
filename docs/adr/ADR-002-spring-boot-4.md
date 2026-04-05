# ADR-002: Spring Boot 4.0.5 as Application Framework

## Status
Accepted

## Context

Spring Boot 4.0.5 (released March 26, 2026) is the latest stable patch of the Spring Boot 4.0 line, built on Spring Framework 7.x and Jakarta EE 11. It is the first Spring Boot generation that Vaadin 25.x supports — Vaadin 25 explicitly requires Spring Boot 4.0.4 or later and does not work with Spring Boot 3.x.

Key breaking changes introduced in Spring Boot 4.0 relevant to this project:
- Requires Jakarta EE 11 (Servlet 6.1); Undertow is no longer supported (Tomcat and Jetty remain)
- Jackson 3 is bundled (package names changed from `com.fasterxml.jackson.*` to `tools.jackson.*`)
- Flyway auto-configuration requires `spring-boot-starter-flyway`; depending on `flyway-core` alone no longer triggers auto-configuration
- Default managed versions of jOOQ (3.19.31) and Flyway (11.14.1) must be overridden for PostgreSQL 18 compatibility (see ADR-005 and ADR-006)

## Decision

Use **Spring Boot 4.0.5** as the application framework.

## Consequences

- Vaadin 25.1.1 is compatible and exclusively requires Spring Boot 4.x — this is the correct generation pairing
- Jackson 3 is the bundled JSON library; no Jackson 2 dependencies must be introduced
- Undertow cannot be used as the embedded servlet container; Tomcat (default) is used
- `spring-boot-starter-flyway` must be declared as the Flyway dependency (not `flyway-core` alone)
- jOOQ and Flyway versions must be explicitly overridden in `pom.xml` (see ADR-005 and ADR-006)
