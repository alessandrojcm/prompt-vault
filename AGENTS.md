# AGENTS.md

## Status

Repo has an established app spine: a Spring Boot 4 REST API in `apps/api`, a TanStack Start React app in `apps/web`, a pnpm workspace API client package in `packages/api-client`, and the shared OpenAPI contract in `openapi/api.yaml`.

## Fast orientation for agents

- Do not rediscover the stack unless the task specifically questions it: backend is Spring Boot 4 + Spring Security + Spring Data JPA + Flyway + MySQL/Testcontainers; frontend is TanStack Start + React + TanStack Router/Query/Form + Mantine; client generation is Hey API from `openapi/api.yaml`.
- Before broad code search, read the established project patterns in [`docs/agents/project-patterns.md`](docs/agents/project-patterns.md); it records the current route/auth/form/API/test conventions.
- Generated artifacts are intentionally not committed: backend OpenAPI output, `packages/api-client/src/generated/**`, `apps/web/dist/**`, and `apps/web/src/routeTree.gen.ts`.
- Prefer existing seams over inventing new ones: OpenAPI contract first, generated API helpers where they fit, TanStack Query for web server state/mutations/cache updates, Mantine UI primitives, TanStack Form for forms, route-level `beforeLoad` guards for web auth, and Spring Security session-cookie auth on the API.

## Toolchain

Managed by [mise](https://mise.jdx.dev/) via `mise.toml`. Run `mise install` to pin versions:

- Java 26.0.1
- Node 26.3.1
- pnpm 11.8.0
- Gradle 9.6.0

Gradle is available via mise and a repo-checked-in wrapper is present. Prefer `./gradlew` for repo tasks unless mise-specific behavior is needed.
Use `mise run` as the standard command surface: `mise run install`, `mise run generate`, `mise run check`, `mise run dev`, `mise run dev:api`, `mise run dev:web`, and `mise run db:up` / `mise run db:down`.
- `mise run check` includes the web app's `oxlint` and `oxfmt --check` steps.

## Project patterns

See [`docs/agents/project-patterns.md`](docs/agents/project-patterns.md) for established architecture, web, API, auth, data, and testing conventions. Treat that document as the first stop before searching for stack or pattern examples.

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
