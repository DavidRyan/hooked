# 🎣 Hooked Backend (Elixir Edition)

A modern Elixir backend for managing fishing catch logs, enriching them with contextual data (weather, trends, predictions), and providing recommendations to clients.

## Setup

1. Install dependencies:
   ```bash
   mix deps.get
   ```

2. Start PostgreSQL:
   ```bash
   docker-compose up -d
   ```

3. Setup database:
   ```bash
   mix ecto.setup
   ```

4. Start server:
   ```bash
   mix phx.server
   ```

## API Endpoints

- `GET /api/user_catches` - List all user catches
- `POST /api/user_catches` - Create new user catch
- `GET /api/user_catches/:id` - Get specific user catch
- `PUT /api/user_catches/:id` - Update user catch
- `DELETE /api/user_catches/:id` - Delete user catch

## Features

- ✅ REST API for user catch management
- ✅ PostgreSQL with Ecto
- ✅ Oban for background jobs
- ✅ Binary IDs for security
- 🔄 Weather enrichment (TODO)
- 🔄 Species recognition (TODO)
- 🔄 Trend analysis (TODO)