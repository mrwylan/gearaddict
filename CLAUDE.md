# GearAddict — build & run

## Stack
- JDK 25 (Temurin), Maven 3.9+
- Spring Boot 4.0.5, Vaadin Flow 25.1.1
- jOOQ 3.21.1 (PostgreSQL 18 dialect), Flyway 12.3.0
- PostgreSQL 18.3

See `docs/adr/` for the full rationale.

## Local development

```bash
# 1. Start Postgres (one-time per session)
docker compose up -d postgres

# 2. Generate jOOQ classes (first build, or after changing migrations)
./mvnw generate-sources

# 3. Run the app (http://localhost:8080)
./mvnw spring-boot:run
```

## Build & test

```bash
./mvnw verify                    # full build + tests
./mvnw -DskipTests package       # skip tests

# Multi-arch OCI image via buildx — see ADR-008
# (single-arch local build: add --load and drop the extra platform)
docker buildx build --platform linux/amd64,linux/arm64 -t gearaddict:local .
```

## Specs
- Use cases: `docs/use-cases/UC-*.md`
- Entity model: `docs/entity-model.md`
- Requirements & UI spec: `docs/requirements.md`
- Wireframes: `docs/wireframes/Wireframes.html`
- ADRs: `docs/adr/`

## Conventions
- Java packages: `app.gearaddict.*`
- Generated jOOQ output: `app.gearaddict.jooq.*` (in `target/generated-sources/jooq`)
- Migration scripts: `src/main/resources/db/migration/V{NNN}__{description}.sql`
- Reserved word `user` is renamed to `users` at the table level.
