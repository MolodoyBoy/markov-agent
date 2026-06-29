# Markov Agent

## Overview

Markov Agent is a Spring Boot service that predicts stock price movements using **Markov Chains** and exposes predictions through an **OpenAI-powered chat interface**.

### How It Works

1. **Data Ingestion** — The service fetches daily stock prices from the [Twelve Data API](https://twelvedata.com/) for companies stored in a PostgreSQL database.
2. **Stock Return Calculation** — Daily stock returns are computed from closing prices.
3. **Markov Chain Construction** — A transition probability matrix is built from historical stock returns over a 36-month window. Stock states are classified as **DOWN**, **STABLE**, or **UP**, with supporting math indices (average, standard deviation).
4. **Prediction** — Given a company, the Markov Chain predicts the most probable future stock state for one or more days forward.
5. **AI Chat Interface** — An OpenAI-based chat client (GPT-5) acts as the user-facing layer. It is strictly scoped to stock prediction queries for a single company and delegates lookups to an internal tool callback (`get_company_stock_state`).

### Key Components

| Package | Description |
|---|---|
| `importer` | Scheduled importers: `NewCompanyImporter` (processes new companies from a queue), `NewDailyStockImporter` (fetches latest stock data), `MarkovChainImporter` (rebuilds the transition matrix) |
| `external_api` | REST client for the Twelve Data API with Resilience4j circuit breaker and retry |
| `calculator` | Computes daily stock returns from raw price data |
| `chat_client` | OpenAI chat client configuration and prompt handling |
| `tool_callback` | Spring AI tool callback that resolves company stock state predictions for the AI model |
| `database` | JDBC-based data access layer for companies, stocks, returns, Markov chains, and math indices |
| `value_object` | Domain records: `Company`, `DailyStock`, `MarkovChain`, `State`, `MathIndex`, etc. |

### Tech Stack

- **Java 25**, **Spring Boot 4**
- **Spring AI** (OpenAI integration)
- **PostgreSQL** + **Flyway** migrations
- **Resilience4j** (circuit breaker & retry)

---

## Environment Variables

The following environment variables **must** be set before starting the service:

| Variable | Description | Example |
|---|---|---|
| `MARKOV_DB_USERNAME` | PostgreSQL database username | `postgres` |
| `MARKOV_DB_PASSWORD` | PostgreSQL database password | `secret` |
| `MARKOV_DB_HOST` | PostgreSQL database host | `localhost` |
| `MARKOV_DB_PORT` | PostgreSQL database port | `5432` |
| `MARKOV_DB_NAME` | PostgreSQL database name | `markov` |
| `OPENAI_API_KEY` | OpenAI API key for GPT-5 chat model | `sk-...` |
| `EXTERNAL_API_KEY` | Twelve Data API key for stock market data | `your_api_key` |

---

## Running the Service

```bash
./gradlew bootRun
```

> **Prerequisites:** Java 25, a running PostgreSQL instance.

