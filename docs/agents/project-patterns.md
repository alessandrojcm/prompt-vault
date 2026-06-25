# Project Patterns

Use this document before broad codebase exploration. The stack and core seams are established; only search when you need task-specific implementation details.

## Architecture

- Prompt Vault is a Spring Boot REST API plus TanStack Start React frontend.
- Use an OpenAPI-first contract at `openapi/api.yaml` as the shared API source of truth.
- Generate Spring Boot interfaces/models with OpenAPI Generator; do not commit generated backend code.
- Generate the TypeScript API client with Hey API in `packages/api-client`; generated output lives under `packages/api-client/src/generated` and must not be committed.
- Keep `packages/api-client/src/index.ts` as a thin re-export surface for Hey API generated SDK, types, TanStack Query helpers, and Valibot schemas.
- Put UI-specific response mapping and client configuration in consuming apps, not `packages/api-client`.
- Configure the web app's Hey API client in `apps/web/src/api-client.ts`: use `credentials: "include"` so browser calls send the session cookie when the API runs on a different origin; SSR/server calls use `PROMPT_VAULT_API_BASE_URL` with a localhost API fallback and must forward the incoming request's `Cookie` header through a TanStack Start `createIsomorphicFn` server implementation.

## Web app patterns

- The web app is TanStack Start with file-based routing under `apps/web/src/routes`.
- The root route layout is `apps/web/src/routes/__root.tsx`; it imports `@mantine/core/styles.css` and wraps the app in `MantineProvider`. The authenticated app shell/sidebar lives in the `/dashboard` layout at `apps/web/src/routes/dashboard.tsx` so login/signup/root auth routes do not render it.
- Use Mantine form/display primitives over custom inline-styled controls.
- Mantine notifications are available in the root layout: `@mantine/notifications/styles.css` is imported after core styles, `<Notifications />` is mounted inside `MantineProvider`, and app code can call `notifications.show(...)` for toast feedback.
- Use TanStack Router `beforeLoad` for auth guards and throw `redirect({ to, replace: true })` for auth redirects.
- The web root `/` is an auth gate only: unauthenticated users redirect to `/login`; authenticated users redirect to `/dashboard`.
- `/dashboard` is the authenticated landing page for both admins and normal users.
- Login success should navigate to `/dashboard` or through the root auth flow; do not restore the old root page as the authenticated home.
- Logout clears the current-user query and navigates to `/login`.
- The generated route tree `apps/web/src/routeTree.gen.ts` is generated and must not be committed.
- Web linting/formatting uses `oxlint` and `oxfmt`; checks ignore `apps/web/dist/**` and `apps/web/src/routeTree.gen.ts`.

## Form and API client patterns

- Generate Valibot schemas from Hey API in `packages/api-client` (`requests`, `responses`, and `definitions`).
- Prefer generated `v*` schemas, such as `vSignupRequest`, as TanStack Form validators for API-backed forms.
- Use TanStack Form for app forms so signup, login, and future forms share form state and submit handling conventions.
- Attach generated request schemas at the form level when validating the whole payload, for example `validators.onChange: vSignupRequest`.
- Use `validators.onSubmitAsync` for mutation submission and navigation side effects.
- Render errors from TanStack Form state instead of parallel React state or helper view models.
- Use Hey API's generated TanStack Query mutation/query helpers where they fit.
- Treat TanStack Query as the default seam for web server state: reads should flow through query helpers/`useQuery`, writes should use generated mutation helpers/`useMutation`, and successful mutation responses should update relevant cached query data with `queryClient.setQueryData` rather than duplicating server rows in local component state.
- If generated query helpers throw for expected control-flow responses such as `401`, call the generated SDK directly in the consuming app so the route can branch on `response.status`.

## API and auth patterns

- Use Spring Security session-cookie authentication for the initial auth slice; CSRF hardening is deferred to later OWASP-focused work.
- API CORS is configured in Spring Security with a `CorsConfigurationSource` for `/api/**`; keep allowed browser origins explicit via `prompt-vault.cors.allowed-origins` / `PROMPT_VAULT_CORS_ALLOWED_ORIGINS` because session-cookie credentials cannot use wildcard origins.
- Signup is `POST /api/signup`: it trims username/email address, preserves password spaces, creates `USER` + `ENABLED`, and returns `400` with `ValidationErrorResponse.fieldErrors[]` for form-friendly validation failures.
- Keep SignupRequest's basic constraints in OpenAPI, including `emailAddress` as `format: email`, so generated frontend Valibot schemas and backend Bean Validation agree on basic shape.
- Trim signup identity fields before Bean Validation and map generated validation failures into `ValidationErrorResponse`.
- Reuse `RequestBodyNormalizationAdvice` + `RequestBodyNormalizer` for DTO normalization before generated Bean Validation.
- Reuse `BeanValidationExceptionHandler` + `BeanValidationFieldMessageResolver` for contract-shaped validation errors with flow-specific messages.
- Login is `POST /api/login`: it authenticates case-insensitive usernames with Spring Security, saves the `SecurityContext` through `HttpSessionSecurityContextRepository`, returns a safe `UserSummary`, and returns `401` with `AuthenticationErrorResponse.message` for invalid credentials.
- Disabled users are rejected during `POST /api/login` through Spring Security's `UserDetails.isEnabled()` / `DisabledException` path; map them to `403` with `AuthenticationErrorResponse.message` (`Your account is disabled. Contact an administrator.`), do not save a `SecurityContext`, and do not establish a new session cookie.
- Current User is `GET /api/user`: it returns `401` when unauthenticated and returns the safe `UserSummary` from the session principal when authenticated.
- Logout is `POST /api/logout`: it requires an authenticated session and invalidates the server session via Spring Security logout handling.
- Existing authenticated sessions are tracked with Spring Security's `SessionRegistry`; manual login must register the created HTTP session so account disablement can expire only the targeted user's sessions.

## Data and admin patterns

- Use MySQL via Docker Compose, Flyway migrations/seed data, and Spring Data JPA for persistence mapping.
- Keep the shared `docker-compose.yml` compatible with Testcontainers Compose-based tests; do not set `container_name` on services used by automated tests.
- The initial `users` Flyway table constrains `email_address` uniquely and stores `role` / `account_status` as MySQL `ENUM`s.
- Case-insensitive username and email uniqueness are enforced with persisted normalized columns (`username_normalized`, `email_address_normalized`) so disabled users still reserve both identities.
- Flyway `V3__seed_admin_user.sql` owns the baseline Admin seed; public signup must never create or promote admins.
- The seeded local/dev admin user is `admin` with password `admin-password123`; keep the migration comment and login coverage updated if changing it.
- User Management lives at `/dashboard/admin/users` under the authenticated dashboard layout and is admin-only; the route guard redirects unauthenticated visitors through the auth gate/login flow and authenticated normal users to `/dashboard`.
- Admin User Management uses `GET /api/admin/users` with an optional `role` enum query parameter (`USER` / `ADMIN`).
- Admin User Management updates account state with `PATCH /api/admin/users/{userId}/status` and a desired `accountStatus`; the endpoint is idempotent, returns the updated user, returns `404` for missing users, and returns `403` when targeting admins or the current user.
- The `/dashboard/admin/users` UI initially requests `role=USER`, hides internal IDs and the role column, shows username/email address/account status/actions, labels enum values for humans, confirms disable actions with a small popover, enables immediately, updates rows in place, and shows toasts.
- Disabling a normal user preserves their data and identity reservations, revokes existing sessions so subsequent current-user checks behave unauthenticated, and does not create audit records in the initial slice.
- Prompt Category catalog is `GET /api/prompt-categories` for authenticated users; baseline categories are seeded by Flyway with stable snake_case slugs, case-insensitive label uniqueness via `label_normalized`, globally unique slugs, and `created_by_user_id` attribution to the seeded Admin.
- Prompt creation is `POST /api/prompts` for authenticated users; requests include `title`, `text`, and `categoryId`, normalize title/text with edge trimming before validation, require an existing Prompt Category, allow duplicate titles, and persist new Prompts as `PRIVATE` with owner/category/timestamps.
- My Prompts listing is `GET /api/users/{userId}/prompts` for authenticated users; `{userId}` must be the current authenticated user, and the response returns all Prompts owned by that user, including private Prompts.
- Owned Prompt detail management uses `GET` / `PATCH` / `DELETE /api/prompts/{promptId}`; ownership is enforced by loading prompts through the current user's id, so missing or non-owned Prompt IDs return `404`.
- Prompt ownership has no admin override: admins can create and manage their own Prompts, but non-owned Prompt detail/update/delete requests still return `404` and must not leak private Prompt content.
- Prompt updates use the same title/text trimming, length validation, and existing-category validation as creation, and owner-driven content/category changes advance `updatedAt`.
- Prompt Visibility transitions use `PATCH /api/prompts/{promptId}/visibility` with a desired `visibility` (`PUBLIC` to Share, `PRIVATE` to Unshare); ownership is enforced like other Prompt mutations, including no admin override, and visibility changes advance `updatedAt`.

## Testing patterns

- Run the standard verification with `mise run check`.
- API integration tests should prefer real MySQL coverage via Testcontainers.
- The shared Compose environment in `AbstractMySqlIntegrationTest` is a manually-started JVM singleton so Spring's cached contexts do not outlive a per-class JUnit container lifecycle.
- Frontend route/auth behavior should be covered at the route/component seam with focused Vitest tests rather than end-to-end browser tests unless browser behavior is the subject of the task.
- Web tests use Vitest 4 Browser Mode with the Playwright Chromium provider; prefer `vitest-browser-react` locators/assertions for component tests instead of jsdom or Testing Library shims.
- Use TanStack Table for web data tables that need client-side table behavior such as filtering; render the headless table model with Mantine table primitives.
