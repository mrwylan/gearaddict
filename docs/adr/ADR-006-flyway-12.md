# ADR-006: Flyway 12.3.0 for Schema Versioning (version override required)

## Status
Accepted

## Context

Database schema changes must be versioned, repeatable, and auditable across all environments (local development, CI, production). Flyway is the leading SQL-based migration tool in the Java ecosystem and integrates natively with Spring Boot.

Flyway 12.3.0 (released March 31, 2026) is the latest stable release and provides full, documented support for PostgreSQL 18. Flyway 11.14.1 — the version managed by Spring Boot 4.0.5 by default — predates PostgreSQL 18's GA release and may lack complete support for PostgreSQL 18-specific migration scenarios.

**Version conflict:** Spring Boot 4.0.5 manages Flyway at version `11.14.1` by default. This version must be explicitly overridden in `pom.xml`:

```xml
<properties>
    <flyway.version>12.3.0</flyway.version>
</properties>
```

**Spring Boot 4 auto-configuration change:** Spring Boot 4 no longer auto-configures Flyway when only `flyway-core` is on the classpath. The `spring-boot-starter-flyway` starter must be declared as the dependency to trigger auto-configuration.

## Decision

Use **Flyway 12.3.0** for all schema versioning, overriding Spring Boot's managed version and using `spring-boot-starter-flyway` as the dependency.

## Consequences

- All schema changes tracked as versioned SQL files in `src/main/resources/db/migration/V*.sql`
- Naming convention: `V{version}__description.sql` (e.g., `V001__create_user_table.sql`)
- Test data managed via Flyway migrations in `src/test/resources/db/migration/`
- Spring Boot's managed `flyway.version` property (11.14.1) must be overridden to `12.3.0` in `pom.xml`
- `spring-boot-starter-flyway` (not `flyway-core`) must be declared in `pom.xml` for auto-configuration
- The `flyway-database-postgresql` module is a required additional dependency in Flyway 12.x
