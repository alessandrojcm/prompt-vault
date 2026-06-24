# AGENTS.md

## Status

Repo now has the auth-slice application spine from issue #2: a Spring Boot 4 API in `apps/api`, a TanStack Start web app in `apps/web`, a pnpm workspace API client package in `packages/api-client`, and the shared contract in `openapi/api.yaml`.

## Toolchain

Managed by [mise](https://mise.jdx.dev/) via `mise.toml`. Run `mise install` to pin versions:

- Java 26.0.1
- Node 26.3.1
- pnpm 11.8.0
- Gradle 9.6.0

Gradle is available via mise and a repo-checked-in wrapper is present. Prefer `./gradlew` for repo tasks unless mise-specific behavior is needed.
Use `mise run` as the standard command surface: `mise run install`, `mise run generate`, `mise run check`, `mise run dev:api`, `mise run dev:web`, and `mise run db:up` / `mise run db:down`.
- `mise run check` includes the web app's `oxlint` and `oxfmt --check` steps.

## Planned architecture

- Prompt Vault is planned as a Spring Boot REST API plus TanStack Start React frontend.
- Use an OpenAPI-first contract at `openapi/api.yaml` as the shared API source of truth.
- Generate Spring Boot interfaces/models with OpenAPI Generator; do not commit generated backend code.
- Generate a TypeScript API client in the pnpm workspace package with Hey API; generated client output lives under `packages/api-client/src/generated` and must not be committed.
- The TanStack Start web app uses `oxlint` and `oxfmt`; ignore generated assets under `apps/web/dist/**` and the generated route tree file `apps/web/src/routeTree.gen.ts` in those checks.
- Use MySQL via Docker Compose, Flyway migrations/seed data, and Spring Data JPA for persistence mapping.
- Keep the shared `docker-compose.yml` compatible with Testcontainers Compose-based tests; do not set `container_name` on services used by automated tests.
- The initial `users` Flyway table constrains `email_address` uniquely and stores `role` / `account_status` as MySQL `ENUM`s.
- Use Spring Security session-cookie authentication for the initial auth slice; CSRF hardening is deferred to later OWASP-focused work.
- The current tracer is `GET /api/user`: the backend returns `401` when unauthenticated, and the web app calls it through the same-origin `/api` dev proxy.
- API integration tests should prefer real MySQL coverage via Testcontainers; the current-user security tracer test boots the app against the repo `docker-compose.yml` MySQL service instead of excluding datasource/Flyway/JPA auto-configuration.

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
