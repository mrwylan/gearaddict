# ADR-003: Vaadin Flow 25.1.1 as UI Framework

## Status
Accepted

## Context

GearAddict is a Java-first project. The team wants to build the entire stack in Java without a separate JavaScript/TypeScript frontend codebase. Vaadin Flow allows building reactive, component-based UIs entirely in Java on the server side, while rendering as a modern web application in the browser.

Vaadin 25.1.1 is the current stable release (late March/early April 2026). It requires Spring Boot 4.0.4 or later (satisfied by ADR-002) and Java 21 or later (satisfied by ADR-001).

Vaadin 25 represents a significant generation shift from Vaadin 24:
- Exclusively targets Spring Boot 4.x (Spring Boot 3.x is not supported)
- Uses Jackson 3 (aligned with Spring Boot 4's bundled version)
- Improved accessibility and WCAG 2.1 AA compliance support

## Decision

Use **Vaadin Flow 25.1.1** as the UI framework, added via `vaadin-spring-boot-starter`.

## Consequences

- All UI code is written in Java — no TypeScript/React required
- Spring Boot 4.0.4+ is enforced at runtime by Vaadin 25.1.x; Spring Boot 4.0.5 satisfies this
- WCAG 2.1 AA compliance (NFR-007) is supported through built-in Vaadin accessibility features
- Vaadin's shadow DOM component architecture requires CSS selectors (not XPath) for Playwright E2E tests
- UI tests use Karibu Testing for server-side unit tests and Playwright with Drama Finder for E2E tests
