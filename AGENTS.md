# AGENTS.md

## Status

Fresh repo with a GitHub remote at `https://github.com/alessandrojcm/prompt-vault.git`. Only scaffolding is present, plus initial domain glossary/ADRs and PRD issue #1 for the login/signup vertical slice.

## Toolchain

Managed by [mise](https://mise.jdx.dev/) via `mise.toml`. Run `mise install` to pin versions:

- Java 26.0.1
- Node 26.3.1
- Gradle 9.6.0

Gradle is available via mise and a repo-checked-in wrapper is present. Prefer `./gradlew` for repo tasks unless mise-specific behavior is needed.

## Planned architecture

- Prompt Vault is planned as a Spring Boot REST API plus TanStack Start React frontend.
- Use an OpenAPI-first contract at `openapi/api.yaml` as the shared API source of truth.
- Generate Spring Boot interfaces/models with OpenAPI Generator; do not commit generated backend code.
- Generate a TypeScript API client in a pnpm workspace package with Hey API, ky, valibot, and TanStack React Query helpers; do not commit generated client code.
- Use MySQL via Docker Compose, Flyway migrations/seed data, and Spring Data JPA for persistence mapping.
- Use Spring Security session-cookie authentication for the initial auth slice; CSRF hardening is deferred to later OWASP-focused work.
- Use mise tasks as the standard command surface for common workflows once implementation begins.

## Agent skills

### Vertical slices and Graphite

- Implement each vertical slice as a Graphite stack.
- The stack name should describe the slice, e.g. `auth-implementation` for the login/signup PRD.
- Each related implementation issue should be a separate entry in that stack.
- Keep stack entries scoped to independently reviewable issue-sized changes while preserving the slice's delivery order.

### Issue tracker

Issues are tracked in GitHub Issues for `alessandrojcm/prompt-vault` via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Use the default Matt Pocock skills triage labels: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, and `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context layout: use root `CONTEXT.md` and root `docs/adr/` for domain language and architectural decisions. See `docs/agents/domain.md`.
