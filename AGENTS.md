# AGENTS.md

## Status

Fresh repo (no commits, no remote, no source yet). Only scaffolding present.

## Toolchain

Managed by [mise](https://mise.jdx.dev/) via `mise.toml`. Run `mise install` to pin versions:

- Java 26.0.1
- Node 26.3.1
- Gradle 9.6.0

Gradle is available via mise (not a repo-checked-in wrapper), so use `gradle` directly rather than `./gradlew` until a wrapper is added.