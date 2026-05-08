# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

- **Build**: `./gradlew build`
- **Run**: `./gradlew bootRun`
- **Run tests**: `./gradlew test`
- **Run a single test**: `./gradlew test --tests "com.markov.agent.SomeTestClass.testMethod"`
- **Docker image (Jib)**: `IMAGE_VERSION=<tag> DOCKER_HUB_PASSWORD=<pw> ./gradlew jib`
- **Local infra**: `cd deploy && docker compose up -d` (starts PostgreSQL on port 5432)

Java 21 (Temurin) is required. Environment variables are loaded from `.env/local.env` for local development.

## Architecture

The application is a Spring Boot 4 service that predicts stock price movements using Markov Chains and exposes predictions through a Telegram bot backed by an OpenAI (GPT-5) chat interface.

### Three-layer structure

The codebase is organized under `com.markov.agent` into three bounded contexts, not traditional horizontal layers:

1. **`data_loader`** — Batch data pipeline (scheduled importers, external API client, stock return calculator, data access). Runs on cron schedules and processes new companies from a queue.
2. **`domain`** — Core business logic: AI chat client (Spring AI + OpenAI), the `CompanyStockStatusToolCallback` (Spring AI tool callback that the LLM invokes), company search, and Markov chain prediction.
3. **`rest_api`** — Telegram webhook controller (`POST /updates`) that receives messages, delegates to `MessageService`, and returns `TelegramResponse`.

A fourth small package, **`application`**, handles startup argument parsing (e.g., `--enable-webhook`).

### Data pipeline flow

`CompanyImporter` (polls every 1 min) → processes new companies from `new_companies_queue` table → fetches historical stock data from Twelve Data API → calculates daily returns → triggers `MarkovChainImporter`.

`DailyStockImporter` (cron midnight) → fetches latest daily stock for existing companies in batches of 50 → calculates returns → triggers `MarkovChainImporter`.

`MarkovChainImporter` → recomputes math indices (AVG, STD_DEV) and the full 3×3 transition probability matrix (DOWN/STABLE/UP) over a 36-month sliding window. Uses PostgreSQL advisory locks to prevent concurrent runs.

### AI prediction flow

Telegram message → `UpdatesController` → `MessageService` → `OpenAIClient` → GPT-5 with system prompt (`prompts/system-prompt.st`) → LLM calls `get_company_stock_state` tool → `CompanyStockStatusToolCallback` classifies latest stock return into a state using AVG ± 0.35×STD_DEV thresholds, then multiplies through the transition matrix for multi-day predictions → returns most probable state.

### Key design patterns

- **Self-injection with `@Lazy`**: `CompanyImporter` and `DailyStockImporter` inject themselves (`self`) to get `@Transactional` proxying on internal method calls.
- **Source interfaces**: All database and external API access goes through interfaces in `source/` packages, with `database/Db*` and `client/` implementations. The domain layer depends on `domain.source.*` interfaces, not data_loader implementations.
- **Resilience4j circuit breaker**: Wraps the Twelve Data API client (`TwelveDataRestClient`). Retry and sleep intervals are hardcoded in importers (12s between chunks, 20s on error, 30s between batches).

### Database

PostgreSQL with Flyway migrations in `src/main/resources/db/migration/`. Key tables: `company`, `daily_stock`, `daily_stock_return`, `markov_chain`, `math_index`, `new_companies_queue`. Flyway is configured with `out-of-order: true`.

All data access uses raw JDBC (`JdbcTemplate`), no ORM.

### CI/CD

GitHub Actions (`.github/workflows/workflow.yml`): on push to main or any PR, builds and pushes a Docker image via Jib to Docker Hub (`molodoyboy777/markov-agent:<commit-sha>`).
