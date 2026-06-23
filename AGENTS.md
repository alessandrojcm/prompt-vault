# AGENTS.md

## Status

Fresh repo with a GitHub remote at `https://github.com/alessandrojcm/prompt-vault.git`. Only scaffolding is present.

## Toolchain

Managed by [mise](https://mise.jdx.dev/) via `mise.toml`. Run `mise install` to pin versions:

- Java 26.0.1
- Node 26.3.1
- Gradle 9.6.0

Gradle is available via mise and a repo-checked-in wrapper is present. Prefer `./gradlew` for repo tasks unless mise-specific behavior is needed.

## Agent skills

### Issue tracker

Issues are tracked in GitHub Issues for `alessandrojcm/prompt-vault` via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Use the default Matt Pocock skills triage labels: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, and `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context layout: use root `CONTEXT.md` and root `docs/adr/` for domain language and architectural decisions. See `docs/agents/domain.md`.
