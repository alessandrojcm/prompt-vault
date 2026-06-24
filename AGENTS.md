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
Use `mise run` as the standard command surface: `mise run install`, `mise run generate`, `mise run check`, `mise run dev`, `mise run dev:api`, `mise run dev:web`, and `mise run db:up` / `mise run db:down`.
- `mise run check` includes the web app's `oxlint` and `oxfmt --check` steps.

## Planned architecture

- Prompt Vault is planned as a Spring Boot REST API plus TanStack Start React frontend.
- Use an OpenAPI-first contract at `openapi/api.yaml` as the shared API source of truth.
- Generate Spring Boot interfaces/models with OpenAPI Generator; do not commit generated backend code.
- Generate a TypeScript API client in the pnpm workspace package with Hey API; generated client output lives under `packages/api-client/src/generated` and must not be committed.
- Keep `packages/api-client/src/index.ts` as a thin re-export surface for Hey API generated SDK, types, and TanStack Query helpers; put UI-specific response mapping in the consuming app.
- Do not configure Hey API runtime client defaults in `packages/api-client`; consuming apps own client configuration so SSR can use an absolute API URL while browser calls can stay same-origin.
- Configure the web app's Hey API client in `apps/web/src/api-client.ts`: browser calls stay same-origin (`/`), while SSR/server calls use `PROMPT_VAULT_API_BASE_URL` with a localhost API fallback.
- The TanStack Start web app uses `oxlint` and `oxfmt`; ignore generated assets under `apps/web/dist/**` and the generated route tree file `apps/web/src/routeTree.gen.ts` in those checks.
- Use Mantine as the web component library: import `@mantine/core/styles.css`, wrap the root in `MantineProvider`, use Mantine `AppShell` for the app-wide shell, and prefer Mantine form/display primitives over custom inline-styled controls.
- Generate Valibot schemas from Hey API in `packages/api-client` (`requests`, `responses`, and `definitions`) and prefer the generated `v*` schemas, such as `vSignupRequest`, as TanStack Form validators for API-backed forms.
- Use TanStack Form for app forms so signup/login/future forms share form state and submit handling conventions; attach generated request schemas at the form level when validating the whole payload (for example `validators.onChange: vSignupRequest`), use `validators.onSubmitAsync` for mutation submission/navigation side effects, and render errors from TanStack Form state instead of parallel React state or helper view models.
- Use Hey API's generated TanStack Query mutation/query helpers where they fit; if generated query helpers throw for expected control-flow responses such as `401`, call the generated SDK directly in the consuming app so the route can branch on `response.status`.
- Use MySQL via Docker Compose, Flyway migrations/seed data, and Spring Data JPA for persistence mapping.
- Keep the shared `docker-compose.yml` compatible with Testcontainers Compose-based tests; do not set `container_name` on services used by automated tests.
- API CORS is configured in Spring Security with a `CorsConfigurationSource` for `/api/**`; keep allowed browser origins explicit via `prompt-vault.cors.allowed-origins` / `PROMPT_VAULT_CORS_ALLOWED_ORIGINS` because session-cookie credentials cannot use wildcard origins.
- The initial `users` Flyway table constrains `email_address` uniquely and stores `role` / `account_status` as MySQL `ENUM`s.
- Use Spring Security session-cookie authentication for the initial auth slice; CSRF hardening is deferred to later OWASP-focused work.
- Login is `POST /api/login`: it authenticates case-insensitive usernames with Spring Security, saves the `SecurityContext` through `HttpSessionSecurityContextRepository`, returns a safe `UserSummary`, and returns `401` with `AuthenticationErrorResponse.message` for invalid credentials.
- Disabled users are rejected during `POST /api/login` through Spring Security's `UserDetails.isEnabled()` / `DisabledException` path; map them to `403` with `AuthenticationErrorResponse.message` (`Your account is disabled. Contact an administrator.`), do not save a `SecurityContext`, and do not establish a new session cookie.
- Current User is `GET /api/user`: it returns `401` when unauthenticated and returns the safe `UserSummary` from the session principal when authenticated.
- Logout is `POST /api/logout`: it requires an authenticated session, invalidates the server session via Spring Security logout handling, and the web UI clears the current-user query before navigating to `/login`.
- The web root `/` is an auth gate only: it calls `GET /api/user`, redirects unauthenticated `401` users to `/login`, and redirects authenticated users to `/dashboard`.
- User Management lives at `/admin/users` and is admin-only; the route guard redirects unauthenticated visitors through the auth gate/login flow and authenticated normal users to `/dashboard`.
- Admin User Management uses `GET /api/admin/users` with an optional `role` enum query parameter (`USER` / `ADMIN`) and `PATCH /api/admin/users/{userId}/status` with a desired `accountStatus`; the status endpoint is idempotent, returns the updated user, returns `404` for missing users, and returns `403` when targeting admins or the current user.
- The `/admin/users` UI initially requests `role=USER`, hides internal IDs and the role column, shows username/email address/account status/actions, labels enum values for humans, confirms disable actions with a small popover, enables immediately, updates rows in place, and shows toasts.
- Disabling a normal user preserves their data and identity reservations, revokes existing sessions so subsequent current-user checks behave unauthenticated, and does not create audit records in the initial slice.
- Signup is now `POST /api/signup`: it trims username/email address, preserves password spaces, creates `USER` + `ENABLED`, and returns `400` with `ValidationErrorResponse.fieldErrors[]` for form-friendly validation failures.
- Keep SignupRequest's basic constraints in OpenAPI, including `emailAddress` as `format: email`, so generated frontend Valibot schemas and backend Bean Validation agree on basic shape; trim signup identity fields before Bean Validation and map generated validation failures into `ValidationErrorResponse`.
- Reuse `RequestBodyNormalizationAdvice` + `RequestBodyNormalizer` for DTO normalization before generated Bean Validation, and reuse `BeanValidationExceptionHandler` + `BeanValidationFieldMessageResolver` for contract-shaped validation errors with flow-specific messages.
- Case-insensitive username and email uniqueness are enforced with persisted normalized columns (`username_normalized`, `email_address_normalized`) so disabled users still reserve both identities.
- Flyway `V3__seed_admin_user.sql` owns the baseline Admin seed; public signup must never create or promote admins.
- API integration tests should prefer real MySQL coverage via Testcontainers; the shared Compose environment in `AbstractMySqlIntegrationTest` is a manually-started JVM singleton so Spring's cached contexts do not outlive a per-class JUnit container lifecycle.

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
