# AGENTS.md — Concurrent Order Processing System

This file orients any AI coding agent (Claude Code or otherwise) working in this repository. Read this before generating code. See `plan.md` for the day-by-day build plan this project follows.

## Project context

Take-home assignment for a Senior Backend Engineer interview. Time budget: ~4 build days + 1 buffer day. **The grading signal is depth on a few named hard concurrency problems, not breadth across every feature in the original spec.** Resist the temptation to "complete the spec" — see "Explicitly out of scope" below.

## Tech stack

- Java 21 (virtual threads enabled)
- **Spring Boot 4.0.7**
- **Maven**
- PostgreSQL 18, Flyway for migrations
- **Redis** for read-through caching of the product catalog only (see design decision #5 below — never used as the source of truth for inventory)
- Spring Security + JWT (`io.jsonwebtoken` / jjwt — added manually, not a Spring Initializr starter)
- springdoc-openapi for Swagger UI
- Bucket4j for rate limiting
- JUnit 5 + Mockito + Testcontainers (PostgreSQL module) for tests
- Docker Compose — **mandatory**, must bring up app + Postgres + Redis together via a single `docker compose up`

### Spring Boot 4.x gotchas to watch for
- `@MockBean` / `@SpyBean` are removed in 4.0 — use `@MockitoBean` / `@MockitoSpyBean` instead.
- Jackson 3 is the default JSON library; double-check date/time serialization and property ordering on DTOs rather than assuming 3.x defaults carry over.
- Boot 4 splits into many more focused starter modules — verify the generated `pom.xml` actually includes what you expect; don't assume a starter pulls in something it used to.

## Build & run commands

```bash
./mvnw clean install                      # build + run unit tests
./mvnw spring-boot:run                    # run the app locally
docker compose up -d                      # mandatory: starts app + Postgres + Redis together
./mvnw test -Dtest=*ConcurrencyTest        # run only the concurrency suite
./mvnw flyway:migrate                     # apply migrations manually if needed
```

`docker compose up` must bring up the full stack with healthchecks — the app's `depends_on` should wait for Postgres and Redis to report healthy before starting, not just for the containers to exist.

## Package structure

```
io.github.mohamedmedhat21.order-processing-system
├── config/         Security, async executor (virtual threads), OpenAPI, rate limiting, Redis cache config
├── controller/      REST controllers — thin, no business logic
├── service/         Business logic; transaction boundaries live here
├── repository/      Spring Data JPA repositories
├── domain/          JPA entities
├── dto/             Request/response DTOs — never expose entities directly
├── statemachine/    Order status transition table + validation (see design decision #3)
├── exception/       Custom exceptions + @ControllerAdvice handler
└── audit/           Audit log writer
```

## Non-negotiable design decisions

Don't relitigate these without explicitly flagging the tradeoff first — they were chosen deliberately for the interview narrative, not just for convenience.

1. **Inventory locking** — pessimistic row lock (`SELECT ... FOR UPDATE`) inside the reservation transaction. Not optimistic versioning (`@Version`). The story being told is "no customer ever sees a false success," and pessimistic locking is the more defensible choice for last-unit contention.
2. **Async work** — Java 21 virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`) as the `@Async` executor bean, not a classic fixed `ThreadPoolExecutor`.
3. **Order state machine** — the enum alone is not the state machine; it's just the alphabet. The actual machine is: (a) an explicit transition table (`Map<OrderStatus, Set<OrderStatus>>`) defining legal `from → to` pairs, (b) a validation step that rejects any transition not in that table, (c) each transition implemented as its own `@Transactional` method that loads the order, validates, performs the business action (reserve inventory / charge payment / etc.), updates the status, and writes an audit log entry — all atomically. States: `CREATED → INVENTORY_RESERVED → PAYMENT_PROCESSING → PAID → FULFILLED → NOTIFIED` (+ `CANCELLED` / `FAILED`). Don't reach for the Spring State Machine library here — it's overkill for a 6-state linear pipeline on this timeline; hand-rolling it is simpler to test and simpler to explain.
4. **Payment idempotency** — every payment attempt keyed by an idempotency key (order ID is fine). Check for an existing payment record before charging. Never charge twice, even under concurrent retries.
5. **Caching strategy** — Redis-backed Spring Cache abstraction (`@Cacheable` / `@CacheEvict`) on product catalog reads only (`GET /products`, `GET /products/{id}`), with TTL expiry and explicit eviction on admin product updates. **Redis is never the source of truth for inventory counts.** The pessimistic lock against Postgres remains the sole authority for stock decisions — caching inventory quantities would reintroduce the exact race condition this project exists to solve.

## Explicitly out of scope — do not add unless asked

- Kafka / RabbitMQ
- Prometheus / Grafana
- Real-time push (WebSockets) — a polling-friendly status endpoint covers this requirement

If a task seems to call for one of these "because it would be quick," stop and flag it instead of adding it. The README needs to say these were deliberately cut for time; a half-finished integration reads worse than a clean omission.

## Testing priorities, in order

1. **Inventory race-condition test** — N concurrent threads buy the last unit; assert exactly one succeeds. Use Testcontainers with real Postgres — H2's locking semantics won't reproduce row-lock contention reliably.
2. **Payment idempotency test** — duplicate request with the same idempotency key results in exactly one charge.
3. **Async non-blocking test** — order-creation response returns before notification work completes.
4. Standard Mockito unit tests for service-layer logic.

Don't optimize for a uniform coverage percentage — these four tests are worth more than twenty trivial getter/setter tests, and a grader will look for them specifically.

## Conventions

- Controllers return DTOs, never JPA entities directly.
- All business exceptions extend a common `BaseBusinessException`, handled centrally via `@ControllerAdvice` and mapped to correct HTTP status codes.
- Every order state transition writes an audit log entry.
- Migrations are Flyway-versioned (`V1__init.sql`, `V2__seed.sql`, ...). Never edit an already-applied migration — add a new one.
- Don't introduce a new dependency without checking it isn't already covered by something in the stack above.

## Git workflow

Three long-lived branches, each representing a stage of readiness:

- **`dev`** — active development. Don't commit directly here for anything non-trivial; branch off it.
- **`integration`** — must always be in a fully runnable, fully-tested state. This is what `docker compose up` and the full test suite (including the concurrency suite) get validated against before promoting further.
- **`main`** — the submission state. What a reviewer would clone to evaluate the assignment.

**Flow:**
1. `feature/<short-name>` (e.g. `feature/inventory-locking`) branched off `dev` → PR into `dev`.
2. `dev` → PR into `integration`, gated on the full test suite passing (including `*ConcurrencyTest`).
3. `integration` → PR into `main`, gated on the same tests plus a clean `docker compose up` run-through from a fresh clone.

**Conventions:**
- Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`) for commit messages.
- Each PR description references which design decision or build-plan day it implements (see `plan.md`), so the history is self-explaining without needing to read the diff first.
- Never push directly to `integration` or `main` — everything flows through `dev` via PR.

## CI is mandatory — and treat it as a security control, not just process hygiene

Three long-lived branches, each representing a stage of readiness:

- **`dev`** — active development. Don't commit directly here for anything non-trivial; branch off it.
- **`integration`** — must always be in a fully runnable, fully-tested state. This is what `docker compose up` and the full test suite (including the concurrency suite) get validated against before promoting further.
- **`main`** — the submission state. What a reviewer would clone to evaluate the assignment.

**Flow:**
1. `feature/<short-name>` (e.g. `feature/inventory-locking`) branched off `dev` → PR into `dev`.
2. `dev` → PR into `integration`, gated on the full test suite passing (including `*ConcurrencyTest`).
3. `integration` → PR into `main`, gated on the same tests plus a clean `docker compose up` run-through from a fresh clone.

**Conventions:**
- Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`) for commit messages.
- Each PR description references which design decision or build-plan day it implements (see `plan.md`), so the history is self-explaining without needing to read the diff first.
- Never push directly to `integration` or `main` — everything flows through `dev` via PR.

## Definition of done

See `plan.md` for the day-by-day schedule and the README checklist that should ship with the final submission.
