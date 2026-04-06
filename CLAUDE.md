# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build (skip tests)
./mvnw clean package -DskipTests

# Run the application
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=DemoApplicationTests
```

## Architecture

This is a **Spring Boot MCP (Model Context Protocol) server** that exposes stock market data as AI-callable tools, backed by the Alpha Vantage REST API.

**Key design decisions:**
- Runs as a non-web process (`WebApplicationType.NONE`) — no HTTP port is opened at runtime despite `spring-boot-starter-web` being on the classpath.
- Communicates over **stdio** using JSON-RPC 2.0. Stdout must stay clean (all logging is suppressed in `application.properties`) because it is the MCP transport channel.
- There are **two parallel dispatch mechanisms** in the codebase:
  1. `McpServerRunner` — a manual `CommandLineRunner` that reads stdin, pattern-matches on `method` strings, and delegates to `StockService`.
  2. Spring AI `@Tool` annotations on `StockService` + `MethodToolCallbackProvider` in `StockApplication` — the Spring AI MCP auto-registration path.
  Both exist simultaneously; new tools must be added to both `StockService` (as `@Tool` methods) and `McpServerRunner` (as a new `else if` branch).

**Data flow:**  
MCP client → stdin JSON-RPC → `McpServerRunner` → `StockService` → Alpha Vantage API (via reactive `WebClient`) → JSON response → stdout

## Source Files

| File | Role |
|---|---|
| `StockApplication.java` | Entry point; registers `StockService` methods as MCP tool callbacks |
| `McpServerRunner.java` | stdin/stdout JSON-RPC loop; routes methods to `StockService` |
| `StockService.java` | All 8 `@Tool` methods; calls Alpha Vantage, maps responses to flat `JsonNode` |

## Configuration

`src/main/resources/application.properties` holds the Alpha Vantage base URL. The API key is read from the `ALPHA_VANTAGE_API_KEY` environment variable — copy `.env.example` to `.env` and set the value before running locally.

WebClient is configured with a 16 MB in-memory buffer to handle large financial responses.