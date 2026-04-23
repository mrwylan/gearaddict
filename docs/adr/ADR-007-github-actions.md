# ADR-007: GitHub Actions as CI/CD Pipeline

## Status
Accepted

## Context

GearAddict is hosted on GitHub. A CI/CD pipeline is required to automate build verification, test execution, and container image publishing on every push and pull request. GitHub Actions is the native CI/CD platform for GitHub repositories and requires no additional third-party integration.

Alternatives considered:
- **Jenkins** — requires self-hosted infrastructure, significant operational overhead
- **GitLab CI** — would require mirroring the repository to GitLab
- **CircleCI / Travis CI** — viable, but introduce an external dependency when GitHub Actions is already available at no additional cost for public repositories

## Decision

Implement the build and release pipeline as **GitHub Actions** workflows in `.github/workflows/`.

## Consequences

- Zero additional tooling or accounts required — pipeline runs natively in the GitHub repository
- Workflow files are version-controlled alongside source code
- The pipeline will cover at minimum:
  - `ci.yml` — triggered on every push and pull request: compile, unit tests (`./mvnw verify`), Karibu tests
  - `release.yml` — triggered on version tags: build, integration tests (`./mvnw verify -Pit`), multi-arch container image build and push via Docker Buildx (see ADR-008)
- GitHub-hosted runners use Ubuntu (latest); a JDK 25 setup step is required (`actions/setup-java` with `temurin` distribution)
- `release.yml` additionally sets up QEMU (`docker/setup-qemu-action`) and Buildx (`docker/setup-buildx-action`) so `linux/amd64` + `linux/arm64` images can be produced on standard runners
- A PostgreSQL 18.3 service container must be declared in the workflow for integration tests
- Secrets for the container registry (e.g., `REGISTRY_USER`, `REGISTRY_TOKEN`) must be configured in GitHub repository settings
