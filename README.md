# Concurrent Order Processing System

Senior Backend Engineer take-home: a Spring Boot e-commerce backend focused on **correct concurrency** — pessimistic inventory locking, idempotent payments, an explicit order state machine, and async notifications on Java 21 virtual threads.

**Stack:** Java 21 · Spring Boot 4.0.7 · PostgreSQL 18 · Redis 7 · Flyway · JWT · Bucket4j · Testcontainers · Docker Compose

---

## Quick start (Docker Compose)

Requires [Docker Desktop](https://www.docker.com/products/docker-desktop/) (or any Docker Engine with Compose v2).

```bash
git clone https://github.com/MohamedMedhat21/order-processing-system.git
cd order-processing-system
cp .env.example .env
docker compose up -d --wait
```

This starts **app + PostgreSQL + Redis** in one command. Flyway migrations and seed data run on app startup.

| Service  | URL |
|----------|-----|
| API      | http://localhost:8080 |
| Swagger  | http://localhost:8080/swagger-ui.html |
| Health   | http://localhost:8080/actuator/health |

### Seed users

| Email | Password | Role |
|-------|----------|------|
| `admin@example.com` | `password` | ADMIN |
| `customer@example.com` | `password` | CUSTOMER |

Product **Webcam HD** (id `5`) is seeded with **1 unit** — used by the inventory race test.

---

## Local development (without Docker for the app)

Run Postgres and Redis via Compose, then start the app with Maven:

```bash
docker compose up -d postgres redis --wait
./mvnw spring-boot:run
```

Or point `SPRING_DATASOURCE_*` / `SPRING_DATA_REDIS_*` at your own instances (see `application.yaml`).

---

## Build & test

```bash
./mvnw clean install          # compile + unit/integration tests
./mvnw clean verify             # full suite (requires Docker for Testcontainers)
./mvnw test -Dtest=*ConcurrencyTest   # concurrency tests only
./mvnw test -Dtest=OrderSubmissionBenchmarkTest -Dsurefire.excludedGroups=  # optional benchmark
```

The concurrency suite uses **real PostgreSQL via Testcontainers** — H2 row-lock semantics are not a substitute for the inventory race test.

---

## Concurrency test suite

These are the tests a reviewer should look for first:

| Test | What it proves |
|------|----------------|
| `InventoryConcurrencyTest` | 50 threads buy the last unit → exactly **one** succeeds |
| `PaymentIdempotencyConcurrencyTest` | 20 concurrent payment attempts → **one** charge row |
| `AsyncNonBlockingConcurrencyTest` | HTTP path returns `FULFILLED` before async `NOTIFIED` completes |

Run them:

```bash
./mvnw test -Dtest=InventoryConcurrencyTest,PaymentIdempotencyConcurrencyTest,AsyncNonBlockingConcurrencyTest
```

---

## Performance benchmark

`OrderSubmissionBenchmarkTest` submits **30 concurrent orders** (product id `1`, ample stock) and logs wall time, per-order latency, and throughput. It is tagged `@Tag("benchmark")` and **excluded from default `mvn verify`** so CI stays fast.

```bash
./mvnw test -Dtest=OrderSubmissionBenchmarkTest -Dsurefire.excludedGroups=
```

**Sample output** (local run, Testcontainers + Postgres 18, 30 threads):

```
Benchmark: 30 concurrent order submissions — wall ~1840 ms, 28 succeeded, avg per-order ~1430 ms, ~15/s throughput
```

Numbers vary by machine and Docker overhead; use the command above to reproduce on your hardware.

---

## API overview

All protected routes require `Authorization: Bearer <access_token>` from `POST /api/v1/auth/login`.

| Area | Endpoints |
|------|-----------|
| Auth | `POST /api/v1/auth/register`, `/login`, `/refresh`, `/logout` |
| Products | `GET /api/v1/products`, `GET /api/v1/products/{id}` (Redis-cached) |
| Orders | `POST /api/v1/orders`, `GET /api/v1/orders`, `GET /api/v1/orders/{id}/status` |
| Admin | `GET /api/v1/admin/orders`, `PUT /api/v1/admin/orders/{id}/status`, `GET /api/v1/admin/reports/daily`, `GET /api/v1/admin/inventory/low-stock` |

Interactive docs: **http://localhost:8080/swagger-ui.html** — use the **Authorize** button with a JWT.

---

## Design decisions

### Pessimistic inventory locking (not optimistic `@Version`)

Inventory reservation runs inside a transaction with `SELECT … FOR UPDATE` on inventory rows (product ids locked in sorted order to avoid deadlocks). **Postgres is the sole authority for stock** — a customer never gets a false success when the last unit is contested.

Optimistic locking with retry storms was considered and rejected here: under last-unit contention, retries add latency and still expose a window where multiple clients believe they succeeded.

### Hand-rolled order state machine

States: `CREATED → INVENTORY_RESERVED → PAYMENT_PROCESSING → PAID → FULFILLED → NOTIFIED` (+ `CANCELLED` / `FAILED`).

The enum is only the alphabet. The machine is:

1. An explicit transition table (`OrderTransitionValidator`)
2. Each legal transition as its own `@Transactional(REQUIRES_NEW)` method in `OrderStateMachine`
3. Business action + status update + audit log in the same transaction

Illegal jumps (e.g. `CREATED → PAID`) are rejected before any side effect runs.

### Payment idempotency

Every charge is keyed by `order-{orderId}`. A unique DB constraint plus a read-before-write (and retry on `DataIntegrityViolationException`) guarantees **at most one charge** even under concurrent retries.

### Virtual threads for async work

`Executors.newVirtualThreadPerTaskExecutor()` backs `@Async` notification delivery and admin report generation (`CompletableFuture`). Order creation returns after `FULFILLED`; notification to `NOTIFIED` runs in the background.

### Redis — catalog only, never inventory

`@Cacheable` on product reads with TTL and `@CacheEvict` on admin updates. **Inventory quantities are never cached** — that would reintroduce the race this project exists to solve.

### Rate limiting

Bucket4j token bucket on `POST /api/v1/orders` (default 10/min per authenticated user), applied after JWT authentication.

---

## Deliberately out of scope

Cut for time; a half-finished integration would read worse than a clean omission:

| Feature | Why skipped |
|---------|-------------|
| **Kafka / RabbitMQ** | Polling-friendly order status endpoint covers tracking; message broker is bonus scope |
| **Prometheus / Grafana** | Actuator health is enough for compose smoke; metrics stack is bonus scope |
| **WebSockets** | `GET /api/v1/orders/{id}/status` supports polling clients |

Redis **is** included — but only for product catalog caching, not as an inventory source of truth.

---

## Branch strategy & CI

Three long-lived branches:

```
feature/*  →  dev  →  integration  →  main
```

- **`dev`** — active development
- **`integration`** — must always build, test, and compose cleanly
- **`main`** — submission / reviewer clone target

PRs are gated by [`.github/workflows/ci.yml`](.github/workflows/ci.yml):

| Job | When | What |
|-----|------|------|
| Build & test | PRs to `integration` / `main` | `./mvnw clean verify` (incl. concurrency suite) |
| CodeQL (default setup) | PRs and default branch | GitHub-managed code scanning (Settings → Code Security) |
| Compose smoke | PRs to `main` only | `docker compose up -d --wait` + health check |

Never push directly to `integration` or `main`.

---

## Project layout

```
src/main/java/.../order-processing-system/
├── config/          Security, async (virtual threads), Redis cache, rate limit, OpenAPI
├── controller/      Thin REST layer
├── service/         Business logic & transaction boundaries
├── repository/      Spring Data JPA
├── domain/          JPA entities
├── dto/             Request/response DTOs
├── statemachine/    Transition table + OrderStateMachine
├── exception/       BaseBusinessException + @ControllerAdvice
└── audit/           Audit log writer
```

See [`AGENTS.md`](AGENTS.md) for agent-oriented conventions and [`plan.md`](.cursor/plans/plan.md) for the day-by-day build plan.
