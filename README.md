# Hooked

Hooked is a fishing log application: anglers record catches and "skunks" (no-catch outings) from their phone, and the backend enriches each entry with location, weather, EXIF, and species data so the user gets richer stats and AI-driven insights over time.

## Repository layout

| Path | Stack | Purpose |
| --- | --- | --- |
| `backend/` | Elixir / Phoenix 1.7 | REST API, Postgres persistence, Oban workers for enrichment, S3 uploads via ExAws. Deployed to Fly.io. |
| `composeApp/` | Kotlin Multiplatform | Shared mobile app shell for Android and iOS. |
| `iosApp/` | Swift / SwiftUI + Xcode | iOS host app and platform-specific UI. |
| `modules/` | Kotlin Multiplatform | Feature modules split into `data`, `domain`, `presentation` for `auth`, `catches`, `skunks`, plus shared `core`. |
| `frontend/` | React 19 + Vite + TypeScript | Web frontend. |
| `chat-server/` | Kotlin / Ktor | WebSocket chat server bridging the OpenAI Java SDK and the MCP Kotlin SDK. |
| `mcp-server/` | Kotlin / Exposed | MCP server exposing the Postgres data layer to model clients. |
| `infra/` | Fly.io / Docker | Deployment configuration. |
| `api-tests/` | HTTP test scripts | Manual API smoke tests. |
| `docs/` | Markdown | Additional design notes. |

Top-level docs worth reading first: `AGENTS.md` (architecture overview), `LOGGING.md` (logging conventions), `mobile_deploy.md` (mobile release process), `backend/DEPLOYMENT.md` (backend release process).

## Backend

Phoenix app under `backend/`. Contexts live in `lib/hooked_api/contexts/` (`accounts`, `catches`, `skunks`); enrichment pipelines live in `lib/hooked_api/enrichers/` and run as Oban workers (`workers/catch_enrichment_worker.ex`, `workers/skunk_enrichment_worker.ex`). Health endpoint: `/api/health`.

```bash
cd backend
mix setup            # deps.get + ecto.create + migrate + seed
mix test             # full test suite (sandboxed Postgres, Oban manual mode)
mix test test/foo_test.exs:42
mix phx.server       # local dev server
```

Configuration is read from Fly.io secrets in production and a gitignored `.env` locally. Never commit secrets.

## Mobile (Kotlin Multiplatform)

Modules follow MVI with strict `data` / `domain` / `presentation` separation. Shared logic lives in `commonMain`; platform code in `androidMain` and `iosMain`. UI is Jetpack Compose on Android and SwiftUI on iOS.

```bash
./gradlew :composeApp:assembleDebug          # Android build
./gradlew :modules:catches:domain:test       # module-level tests
```

iOS builds are driven from `iosApp/iosApp.xcworkspace` (run `pod install` in `iosApp/` first). Mapbox downloads require `MAPBOX_DOWNLOADS_TOKEN` in `.env`, gradle properties, or the environment.

## Web frontend

```bash
cd frontend
npm install
npm run dev
npm run build
npm run lint
```

## Chat and MCP servers

`chat-server/` is a Ktor WebSocket service that proxies between mobile clients, the OpenAI Java SDK, and an MCP server. `mcp-server/` is a standalone MCP server backed by Postgres via Exposed + HikariCP. Both build with Gradle Shadow:

```bash
./gradlew :chat-server:shadowJar
./gradlew :mcp-server:shadowJar
```

## Data flow

1. User records a catch or skunk in the mobile app; state is held by a feature ViewModel.
2. App POSTs to `/api/user_catches` or `/api/user_skunks`.
3. Phoenix controller calls the relevant context; schemas validate input.
4. Worker (`SkunkEnrichmentWorker`, `CatchEnrichmentWorker`, etc.) fetches geo / weather / EXIF / species data and persists enrichment.
5. Aggregated data feeds insights (`fishing_insights_service.ex`) and AI endpoints (`ai_controller.ex`).
6. Mobile receives the enriched record; analytics operate on the joined data.

## Conventions

- Strict context boundaries: skunks and catches never mix in feeds; only in stats.
- Errors flow through the Phoenix fallback controller on the backend and structured logging on mobile. Never swallow exceptions.
- No secrets in code or logs.
- New conceptually distinct features go in their own context (backend) or module (mobile).

## Deployment

Backend deploys to Fly.io via GitHub Actions on tag push; migrations run from `rel/overlays/bin/migrate`. See `backend/DEPLOYMENT.md` for the full pipeline and `mobile_deploy.md` for the mobile release process.
