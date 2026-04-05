# ADR-005: jOOQ 3.21.1 for Data Access (version override required)

## Status
Accepted

## Context

The project requires a type-safe, compile-time-checked approach to SQL queries. jOOQ generates Java classes directly from the database schema and provides a fluent DSL that mirrors SQL, avoiding the abstraction leakage and N+1 query problems common in ORM frameworks like JPA/Hibernate.

jOOQ 3.21.1 (released March 26, 2026) is the first jOOQ version with full PostgreSQL 18 support via `SQLDialect.POSTGRES_18`. Prior versions (including jOOQ 3.19.31, the version managed by Spring Boot 4.0.5) do not support PostgreSQL 18 and will fail to connect or generate incorrect SQL for PostgreSQL 18-specific features.

**Version conflict:** Spring Boot 4.0.5 manages jOOQ at version `3.19.31` by default. This version must be explicitly overridden in `pom.xml`:

```xml
<properties>
    <jooq.version>3.21.1</jooq.version>
</properties>
```

## Decision

Use **jOOQ 3.21.1** for all database access, overriding Spring Boot's managed version. JPA and Hibernate must not be introduced.

## Consequences

- Type-safe SQL at compile time — schema changes that break queries are caught at build time (after code generation)
- jOOQ code generation runs against the PostgreSQL 18.3 schema during the build (`jooq-codegen-maven` plugin)
- Spring Boot's managed `jooq.version` property (3.19.31) must be overridden to `3.21.1` in `pom.xml`
- `spring-boot-starter-jooq` is used as the integration dependency
- No `@Entity`, `@Repository`, or JPA annotations anywhere in the codebase
- jOOQ DSL context is configured with `SQLDialect.POSTGRES_18`
