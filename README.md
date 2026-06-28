# Prompt Vault

Prompt Vault is a full-stack prompt management app with a Spring Boot REST API, a TanStack Start React frontend, and an OpenAPI-first contract shared across the stack.

## Tech stack

- **Backend:** Java 26, Spring Boot 4, Spring Security, Spring Data JPA, Flyway, MySQL, Testcontainers, Gradle
- **Frontend:** React 19, TanStack Start, TanStack Router, TanStack Query, TanStack Form, TanStack Table, Mantine, Vite, Vitest Browser Mode
- **API contract and clients:** OpenAPI, OpenAPI Generator for Spring interfaces/models, Hey API for the TypeScript client, Valibot schemas
- **Tooling:** mise, pnpm, Docker Compose, oxlint, oxfmt

## Architecture

The project is contract-driven:

- `openapi/api.yaml` is the shared API source of truth.
- The backend generates Spring interfaces and models from the OpenAPI contract during Gradle builds.
- `packages/api-client` generates the TypeScript API client, TanStack Query helpers, and Valibot schemas with Hey API.
- `apps/web` consumes the generated client and schemas instead of hand-writing request/response types.

Generated artifacts are intentionally not committed, including backend OpenAPI output, `packages/api-client/src/generated/**`, `apps/web/dist/**`, and `apps/web/src/routeTree.gen.ts`.

## Prerequisites

Install:

- [mise](https://mise.jdx.dev/) for pinned tool versions
- Docker / Docker Desktop for the local MySQL database

The repository pins these tools through `mise.toml`:

- Java 26.0.1
- Node 26.3.1
- pnpm 11.8.0
- Gradle 9.6.0

## Environment variables

Create a local `.env` file if you need to override defaults or provide API keys. The main variables are:

```sh
VITE_API_URL=http://localhost:8080
PROMPT_VAULT_API_BASE_URL=http://localhost:8080
OPENROUTER_API_KEY=
```

- `VITE_API_URL`: browser-facing API base URL used by the web app.
- `PROMPT_VAULT_API_BASE_URL`: server-side API base URL used by TanStack Start SSR/server functions.
- `OPENROUTER_API_KEY`: optional OpenRouter key for AI responses. If omitted, the app returns a fake response.

For local development, the API database settings have defaults that match `docker-compose.yml`:

- database: `prompt_vault`
- username: `prompt_vault`
- password: `prompt_vault`
- host/port: `localhost:3306`

## Running locally

Install pinned tools and dependencies:

```sh
mise install
mise run install
```

Start MySQL with Docker Compose:

```sh
mise run db:up
```

Generate API artifacts:

```sh
mise run generate
```

Run the API and web app:

```sh
mise run dev
```

This starts:

- API: `http://localhost:8080`
- Web app: `http://localhost:3000`

You can also run each app separately:

```sh
mise run dev:api
mise run dev:web
```

Stop the local database when finished:

```sh
mise run db:down
```

## Verification

Run the standard project checks:

```sh
mise run check
```

Build everything:

```sh
mise run build
```

## Seeded data

Flyway seeds local development data when the API starts against an empty database.

### Users

| Username | Email | Role | Password |
| --- | --- | --- | --- |
| `admin` | `admin@promptvault.local` | `ADMIN` | `admin-password123` |
| `user1` | `user1@user.com` | `USER` | `password123` |
| `user2` | `user2@user.com` | `USER` | `password123` |

### Prompt categories

- Coding
- Research
- Cybersecurity
- HR
- Legal
- Personal Productivity

### Policy keywords

- Secret
- API Key
- Password
- Money
- Credit


### Prompts
- 6 prompts in total
- 2 for each user, one public, one private
- Some of them will be flagged