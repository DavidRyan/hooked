# Hooked

Hooked is a fishing log application. Anglers record catches and "skunks" (no-catch outings) from their phone; a backend enriches each entry with location, weather, EXIF, and species data; an AI agent gives the user insights, conditional patterns, and a fishing-expert chat backed by their own history.

## Repository layout

| Path | Stack | Purpose |
| --- | --- | --- |
| [`backend/`](#backend) | Elixir / Phoenix 1.7 | REST API, Postgres persistence, Oban enrichment workers, S3 uploads, AI services. Deployed to Fly.io. |
| [`composeApp/`](#mobile-shell--composeapp) | Kotlin Multiplatform | App shell — navigation, DI, screen wiring, Chat feature. Builds the Android `:app` and the iOS framework. |
| [`modules/`](#mobile-feature-modules--modules) | Kotlin Multiplatform | Feature modules split into `data` / `domain` / `presentation` for `auth`, `catches`, `skunks`, plus shared `core`. |
| [`iosApp/`](#ios-host--iosapp) | Swift / SwiftUI + Xcode | iOS host, native Mapbox bridge, Info.plist. |
| [`frontend/`](#web-frontend--frontend) | React 19 + Vite + TypeScript | Web frontend. |
| [`chat-server/`](#chat-server--chat-server) | Kotlin / Ktor | WebSocket service that authenticates with Hooked, bridges OpenAI ↔ MCP, and runs the agent loop. |
| [`mcp-server/`](#mcp-server--mcp-server) | Kotlin / Exposed | MCP server exposing the user's fishing data, weather, biology, and web search as tools. |
| [`infra/`](#deployment) | Fly.io / Docker | Deployment configuration. |
| [`api-tests/`](#api-tests--api-tests) | HTTP test scripts | Manual API smoke tests. |
| [`docs/`](#docs--docs) | Markdown | Additional design notes. |

Top-level docs worth reading first: `AGENTS.md` (architecture overview), `LOGGING.md` (logging conventions), `mobile_deploy.md` (mobile release process), `backend/DEPLOYMENT.md` (backend release process), `TODO.md` (open work), `ui_refresh.md` (UI refresh plan).

---

## Backend

Phoenix REST API. Contexts in `lib/hooked_api/contexts/` (`accounts`, `catches`, `skunks`) own their schemas and CRUD; enrichment runs in `lib/hooked_api/enrichers/` via Oban workers. The `RibbonInsightService` generates a short AI-powered insight for the timeline ribbon and caches it per-user-per-day in `:persistent_term`.

**Key paths:**
- `lib/hooked_api_web/controllers/` — `AuthController`, `UserCatchController`, `UserSkunkController`, `AiController`, `InsightsController`
- `lib/hooked_api_web/router.ex` — routes (public `/api/health`, rate-limited `/api/auth/*`, authenticated rest)
- `lib/hooked_api/contexts/` — `accounts`, `catches`, `skunks` (each with `schemas/`)
- `lib/hooked_api/workers/` — `CatchEnrichmentWorker`, `SkunkEnrichmentWorker`
- `lib/hooked_api/services/` — `FishingInsightsService`, `RibbonInsightService`, AI providers
- `priv/repo/migrations/` — Ecto migrations

**Run:**
```bash
cd backend
mix setup            # deps.get + ecto.create + migrate + seed
mix phx.server       # local dev server on :4000
mix test             # full test suite (sandboxed Postgres, Oban in manual mode)
mix test test/foo_test.exs:42   # single test
```

**Config:** Fly.io secrets in production, a gitignored `.env` locally (`DATABASE_URL`, `SECRET_KEY_BASE`, `JWT_SECRET`, `OPENAI_API_KEY`, `OPENWEATHER_API_KEY`, `AWS_*`, `S3_*`). Never commit secrets.

---

## Mobile shell — `composeApp/`

The Kotlin Multiplatform app shell. Owns top-level navigation, DI, theme, the bottom navigation bar, the AuthStateManager that routes login → onboarding → tabs, and the Chat feature (screen, view-model, WebSocket client).

**Key paths:**
- `src/commonMain/kotlin/HookedApp.kt` — root `NavHost`, Scaffold + bottom bar, route table
- `src/commonMain/kotlin/HookedBottomBar.kt` — five-tab nav (Log, Map, Chat, Insights, Profile)
- `src/commonMain/kotlin/AuthStateManager.kt` — initial routing based on auth + onboarding state
- `src/commonMain/kotlin/com/hooked/chat/` — `ChatScreen`, `ChatViewModel`, `ChatSocketClient`, intents/state/effects
- `src/commonMain/kotlin/di/Modules.kt` — Koin modules for presentation, data, use cases
- `src/androidMain/kotlin/.../MainActivity.kt` — Android entry, BuildConfig → AppConfig hand-off

**Run:**
```bash
./gradlew :composeApp:assembleLocalDebug          # Android local-flavor APK
./gradlew :composeApp:installLocalDebug           # build + install on attached device
./gradlew :composeApp:compileLocalDebugKotlinAndroid   # type-check only
```

**Config:** `MAPBOX_ACCESS_TOKEN` and per-flavor `API_BASE_URL` / `CHAT_BASE_URL` injected via `BuildConfig` from gradle properties / `.env`. Flavor `local` points at `http://10.0.2.2:4000/api` and `ws://10.0.2.2:8080` (emulator host loopback); `prod` points at the deployed Fly.io URLs.

---

## Mobile feature modules — `modules/`

One module per feature × layer. Strict separation: `domain` defines entities/repos/use-cases, `data` provides DTOs/API services/repo implementations, `presentation` holds Compose screens + MVI view models. UI is Jetpack Compose on Android and SwiftUI on iOS via the shared KMP framework.

**Layout:**
- `core/{domain,presentation}` — shared logger, theme/colors/typography, config, animation specs, navigation `Screens` sealed class, location/map/photo expect/actual, toast manager
- `auth/{data,domain,presentation}` — login, register, onboarding pager, profile, token storage, preferences PATCH
- `catches/{data,domain,presentation}` — timeline grid, hero detail, weather badge, intelligence ribbon, stat strip, map screen, stats screen, submit catch
- `skunks/{data,domain,presentation}` — submission only (skunks are server-side data only in v1 UI)

**Key surfaces:**
- `CatchesScreen` — day-grouped 2-col grid with pill overlays, hero detail with shared-element transition
- `IntelligenceRibbon` — AI-generated `{headline, body}` strip above the timeline; taps into Chat with the headline as starter prompt
- `OnboardingScreen` — three-step pager (home water, target species, permissions)

**Run:**
```bash
./gradlew :modules:catches:domain:test            # module-level tests
./gradlew :modules:auth:presentation:test
```

---

## iOS host — `iosApp/`

SwiftUI entry that hosts the shared KMP framework. Owns the iOS-specific Mapbox SwiftUI bridge (`MapboxMapProvider.swift`) and Info.plist (permission descriptions, API base URLs, Mapbox token).

**Run:**
```bash
cd iosApp
pod install                                       # one-time, fetches CocoaPods
open iosApp.xcworkspace                           # build/run from Xcode
```

**Config:** `MAPBOX_DOWNLOADS_TOKEN` for the framework pull (gradle property / env / `.env`). Runtime values read from Info.plist (`API_BASE_URL`, `CHAT_BASE_URL`, `MAPBOX_ACCESS_TOKEN`).

---

## Web frontend — `frontend/`

React + Vite + TypeScript. Mirrors the mobile feature shape (`features/`, `pages/`, `services/`, `types/`) and consumes the same `/api/*` REST endpoints.

**Run:**
```bash
cd frontend
npm install
npm run dev                                       # vite dev server
npm run build                                     # production bundle
npm run lint
```

---

## chat-server — `chat-server/`

Standalone Ktor service (not in the root Gradle settings — built from its own `chat-server/build.gradle.kts`). Hosts a `/chat` WebSocket. Each session: validates the user's JWT against Hooked `/auth/me`, spawns a per-user MCP subprocess scoped to that email, runs an OpenAI agent loop that chains MCP tools before answering, and streams `{type: tool_call | message | error}` events to the mobile client.

**Key paths:**
- `src/main/kotlin/com/hooked/chat/Main.kt` — server boot, per-WS auth + MCP spawn, system prompt
- `src/main/kotlin/com/hooked/chat/TokenValidator.kt` — calls Hooked `/auth/me` to validate a JWT
- `src/main/kotlin/com/hooked/chat/McpClient.kt` — stdio client to the MCP subprocess, forwards env vars
- `src/main/kotlin/com/hooked/chat/AgentLoop.kt` — OpenAI tool-call loop (chat-completions API)
- `src/main/kotlin/com/hooked/chat/ToolConverter.kt` — MCP `Tool` → OpenAI `ChatCompletionTool`

**Run:**
```bash
./gradlew :chat-server:shadowJar                  # fat jar
MCP_SERVER_JAR=mcp-server/build/libs/hooked-mcp-server.jar \
  DATABASE_URL="jdbc:postgresql://localhost/hooked_api_dev?user=postgres&password=postgres" \
  HOOKED_API_URL=http://localhost:4000/api \
  OPENAI_API_KEY=sk-... \
  OPENWEATHER_API_KEY=... \
  TAVILY_API_KEY=tvly-... \
  java -jar chat-server/build/libs/hooked-chat-server.jar
```

**Config:** `OPENAI_MODEL` (default `gpt-4o`), `PORT` (default `8080`). The mobile `local` flavor expects `ws://10.0.2.2:8080`.

---

## mcp-server — `mcp-server/`

Standalone Kotlin / JVM MCP server. Speaks MCP over stdio, spawned as a subprocess by `chat-server` (one per WebSocket session, scoped by `USER_EMAIL`). 21 tools across history-derived analytics, weather, biology, and web search. Database access via Exposed + HikariCP (pool size 3).

**Key paths:**
- `src/main/kotlin/com/hooked/mcp/Main.kt` — boot, user-email resolution, tool registration, stdio transport
- `src/main/kotlin/com/hooked/mcp/Database.kt` — Hikari + Exposed connect
- `tables/` — Exposed mappings (`CatchesTable`, `SkunksTable`, `UsersTable`)
- `models/` — typed shapes for tool outputs
- `resources/` — `fishing://profile` MCP resource (always-available user summary)
- `tools/` — 21 tool handlers (one file each)

**Tools (21):**
- *Data access (5):* `list_catches`, `get_catch`, `get_catch_stats`, `list_skunks`, `search_by_location`
- *Pattern analysis (11):* `get_species_profile`, `get_best_conditions`, `get_monthly_breakdown`, `get_personal_records`, `compare_periods`, `get_conditional_breakdown`, `get_location_profile`, `find_outlier_catches`, `evaluate_window`, `compare_locations`, `get_dawn_dusk_index`
- *Weather (2):* `get_catch_weather`, `get_live_weather`
- *Non-history knowledge (3):* `get_solunar`, `get_species_ecology`, `web_search` (Tavily)

**Run:**
```bash
./gradlew :mcp-server:shadowJar                   # fat jar
# Normally spawned by chat-server. To smoke-test directly:
USER_EMAIL=you@example.com \
  DATABASE_URL="jdbc:postgresql://localhost/hooked_api_dev?user=postgres&password=postgres" \
  java -jar mcp-server/build/libs/hooked-mcp-server.jar
```

**Config:** `USER_EMAIL`, `DATABASE_URL` required. Optional: `OPENWEATHER_API_KEY` (enables `get_live_weather`), `TAVILY_API_KEY` (enables `web_search`).

---

## API tests — `api-tests/`

HTTP scripts for smoke-testing the backend without going through the mobile app. Useful for verifying a new endpoint or reproducing a bug against a local or remote Phoenix instance.

---

## Docs — `docs/`

Additional design notes, architecture deep-dives, and historical context. Top-level docs (`AGENTS.md`, `LOGGING.md`, `mobile_deploy.md`, `TODO.md`, `ui_refresh.md`) cover the higher-frequency reading.

---

## Data flow

1. **Onboarding:** first-launch users complete a three-step pager (home water, target species, permissions); preferences PATCHed to `/api/auth/preferences`, `onboarding_completed` set to `true`.
2. **Logging:** user records a catch (or skunk) in the mobile app; ViewModel posts multipart to `/api/user_catches` (or `/api/user_skunks`).
3. **Persistence:** Phoenix controller calls the relevant context; schemas validate input; the row lands in Postgres.
4. **Enrichment:** Oban workers (`CatchEnrichmentWorker`, `SkunkEnrichmentWorker`) fetch geo / weather / EXIF / species data and write back to the row.
5. **Browse:** mobile fetches enriched records via `/api/user_catches`, renders them in the day-grouped timeline grid.
6. **Insights ribbon:** `RibbonInsightService` builds a compact stats summary, sends it to OpenAI, parses `{headline, body}` JSON, caches the response per-user-per-day in `:persistent_term`. Mobile `IntelligenceRibbon` displays it; tap navigates to Chat with the headline as the starter prompt.
7. **Chat:** mobile opens a WS to chat-server with `?token=<jwt>`; chat-server validates against Hooked, spawns a per-user MCP subprocess, runs an OpenAI agent loop; tool calls render as inline "looking up …" indicators, final message appears as an assistant bubble.
8. **Web/AI side channels:** the long-form `/api/ai/insights` endpoint powers the Stats screen's typewriter; `web_search` (Tavily) and `get_solunar` / `get_species_ecology` let the agent answer questions independent of personal history.

---

## Conventions

- Strict context boundaries: skunks and catches never mix in feeds, only in stats.
- Errors flow through the Phoenix fallback controller on the backend and structured logging on mobile. Never swallow exceptions.
- No secrets in code or logs. `.env` is gitignored everywhere.
- New conceptually distinct features go in their own context (backend) or module (mobile).
- Logging: use the central `Logger` on mobile (see `LOGGING.md`) and `Logger` / `IO.inspect(label:)` on the backend. Never log PII or tokens.

---

## Deployment

- **Backend:** deploys to Fly.io via GitHub Actions on tag push; migrations run from `rel/overlays/bin/migrate`. See `backend/DEPLOYMENT.md`.
- **Mobile (Android):** `.github/workflows/android-internal-app-sharing.yml` ships every push to `main` to Google Play Internal App Sharing. See `mobile_deploy.md`.
- **Mobile (iOS):** manual builds from Xcode. TestFlight pipeline is a known TODO.
- **chat-server / mcp-server:** local-only for now. Fly.io deploy is planned (`TODO.md` → Chat / MCP section).
