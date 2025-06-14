# OpenCode.md – Repository Guide for Coding Agents

## Build / Lint / Test Commands

- **Kotlin Multiplatform (shared/composeApp):**
  - Build: `./gradlew build`
  - Lint: `./gradlew lint` or `./gradlew lintFix` (applies safe suggestions)
  - Test: `./gradlew test` (note: per-test selection not enabled via `--tests`)  
  - Clean: `./gradlew clean`
- **Rust backend:**
  - Build: `cargo build`
  - Test all: `cargo test`
  - Test single: `cargo test <TESTNAME>`
- For both: run commands from repository root for correct results.

## Code Style Guidelines

- **Imports:** Alphabetize and avoid unused. For Kotlin, use explicit imports over wildcards.
- **Formatting:** Use `ktlint`/Kotlin default for formatting. Rust should use `rustfmt`.
- **Types & Naming:**
  - Kotlin: `CamelCase` for types/classes, `camelCase` for vars/funs, `UPPER_SNAKE_CASE` for constants.
  - Rust: `snake_case` for vars/funs, `CamelCase` for structs/enums, `UPPER_SNAKE_CASE` for constants/statics.
- **Error Handling:** Prefer idiomatic error handling:
  - Kotlin: Use sealed classes or exceptions for errors.
  - Rust: Use `Result` and `Option` types with proper propagation (`?`).
- **General:**
  - Keep functions small, pure, and well-typed.
  - Public symbols must have doc comments.
  - No TODOs or commented-out code committed.
  - No test code or debug output in production logic.

## Tooling/Meta

- No Cursor or Copilot rules found; follow best practices listed above.