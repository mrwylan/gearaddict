# ADR-004: PostgreSQL 18.3 as Database Engine

## Status
Accepted

## Context

GearAddict requires a reliable, open-source relational database. PostgreSQL is the most widely supported open-source RDBMS in the Java ecosystem and is explicitly supported by jOOQ 3.21.x and Flyway 12.x.

PostgreSQL 18.3 (released February 26, 2026) is the latest stable patch of the PostgreSQL 18 line. PostgreSQL 18 introduced significant performance improvements and is fully supported by both jOOQ 3.21.1 (via `SQLDialect.POSTGRES_18`) and Flyway 12.3.0.

Note: jOOQ 3.19.x (Spring Boot 4.0.5's default managed version) does not support PostgreSQL 18 — its maximum tested version is PostgreSQL 15. This requires a version override (see ADR-005).

## Decision

Use **PostgreSQL 18.3** as the relational database engine.

## Consequences

- Full support in jOOQ 3.21.1 via `SQLDialect.POSTGRES_18` — requires overriding Spring Boot's default managed jOOQ version (see ADR-005)
- Full support in Flyway 12.3.0 — requires overriding Spring Boot's default managed Flyway version (see ADR-006)
- Local development and CI require a PostgreSQL 18.3 instance (Docker image: `postgres:18.3`)
- The JDBC driver (`org.postgresql:postgresql`) must be at a version that supports PostgreSQL 18 (42.7.x or later)
