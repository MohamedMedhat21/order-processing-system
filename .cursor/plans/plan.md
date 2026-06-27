# Build Plan — Concurrent Order Processing System

**Context:** Senior Backend Engineer take-home, Java 21 + Spring Boot.
**Time budget:** 2–5 days (planned as 4 build days + 1 buffer day).
**Grading thesis:** the spec describes a full e-commerce platform that no one finishes in this window. What's actually being evaluated is whether the named hard problems are solved correctly and whether you scope sensibly under pressure — not feature count.

---

## 1. Scope decisions

### Build for real
- RESTful API per the spec (users, products, orders, admin)
- PostgreSQL + Flyway migrations + seed data
- JWT auth (customer / admin roles)
- Inventory race-condition prevention (pessimistic locking)
- Idempotent payment processing
- Explicit order state machine
- Async notifications via Java 21 virtual threads
- Rate limiting on order creation
- **Redis-backed caching of product catalog reads** (never inventory — see §2)
- **GitHub Actions CI** (`.github/workflows/ci.yml`) — required, not optional: build+test, CodeQL security scan, and a full-stack compose smoke test gating PRs into `integration`/`main`
- Swagger/OpenAPI docs
- **Docker Compose — mandatory, full stack: app + PostgreSQL + Redis, single `docker compose up`**
- Concurrency-focused test suite (Testcontainers)
- A small benchmark for concurrent order submission

### Deliberately cut — state this explicitly in the README
- Kafka / RabbitMQ (listed as bonus in the spec)
- Prometheus / Grafana (listed as bonus)
- Real-time push (WebSockets) — a polling-friendly status endpoint covers "live tracking" without the added complexity

Cutting these and *saying so* reads as senior judgment. Half-implementing one of them under time pressure reads worse than skipping it cleanly.

---

## 2. Architecture decisions

| Problem | Decision | Why |
|---|---|---|
| Inventory race condition | Pessimistic row lock (`SELECT ... FOR UPDATE`) inside the reservation transaction | Simpler, more defensible story than optimistic retry storms for "last unit" contention. Mention the optimistic-locking alternative in the README and why it was rejected here. |
| Modern concurrency | Java 21 virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`) as the async executor | Spec explicitly calls out "modern Java concurrency libraries" |
| Order pipeline | Hand-rolled state machine: an explicit transition table (`from → to` pairs) + a validation step + each transition as its own `@Transactional` method (validate → business action → update status → audit log). The enum alone (`CREATED → INVENTORY_RESERVED → PAYMENT_PROCESSING → PAID → FULFILLED → NOTIFIED`, + `CANCELLED`/`FAILED`) is just the label set — the table + validation is what makes it a "machine." | Makes "process concurrently without inconsistency" concrete, testable per transition, and prevents illegal status jumps that a free-text/unvalidated field wouldn't catch |
| Payment idempotency | Idempotency key (order ID is sufficient) checked before charging | Guarantees no double-charge even under concurrent retries |
| Catalog reads | Redis-backed Spring Cache (`@Cacheable`/`@CacheEvict`) on `GET /products` and `GET /products/{id}` only | Read-heavy, low-volatility data — good caching candidate. **Never used for inventory counts**, which would reintroduce the exact race condition this project exists to prevent |

---

## 3. Day-by-day

### Day 1 — Foundation
- Project skeleton: layered architecture (controller / service / repository / domain / dto / statemachine / exception / audit)
- **Docker Compose — mandatory, full stack**: app + Postgres + Redis, with healthchecks so the app waits for both before starting
- **GitHub Actions CI** (`.github/workflows/ci.yml`): build+test, CodeQL scan, compose smoke test — set this up *before* the first PR exists
- **Branch protection rules** on `integration` and `main` (repo Settings → Branches): require PR + require the CI jobs above as status checks — this is a manual one-time setting, can't be done via a file
- Flyway migrations for all 8 entities (Users, Products, Orders, OrderItems, Payments, Inventory, Notifications, AuditLogs)
- Seed data
- Global exception handling (`@ControllerAdvice`)

### Day 2 — Core API + Auth
- JWT auth with Spring Security (CUSTOMER / ADMIN roles)
- Product CRUD + pagination
- Redis-backed caching on `GET /products` and `GET /products/{id}` (`@Cacheable`, with `@CacheEvict` on admin product updates)
- Happy-path order creation (no concurrency hardening yet)
- Swagger UI via springdoc-openapi

### Day 3 — Concurrency (the part that's actually being graded)
- Pessimistic-locked inventory reservation
- Order state machine with per-transition transactional methods
- Idempotent payment simulation
- Virtual-thread executor wired into `@Async` notification service
- Rate limiting on the order-creation endpoint (Bucket4j)

### Day 4 — Tests + Reporting + Admin
- **The most important test:** ~50 concurrent threads buying the last unit of a product → assert exactly one succeeds
- Payment idempotency test: duplicate request → exactly one charge
- Daily sales report built with `CompletableFuture`, visibly non-blocking
- Admin endpoints (list/update orders, low-stock alerts)
- Audit log writes on every status transition

### Day 5 — Buffer / polish
- README: setup steps, design-decision rationale (especially the locking-strategy choice), explicit "cut for time" list
- Small benchmark (JUnit test or k6 script) timing N concurrent order submissions — satisfies the "performance benchmarks" deliverable
- Clean Docker run-through from a fresh clone — single `docker compose up` brings up app + Postgres + Redis with no manual steps
- Final Swagger sanity check

---

## 4. Testing priorities (not uniform coverage)

1. Inventory race-condition test (Testcontainers + real Postgres — not H2)
2. Payment idempotency test
3. Async non-blocking test (response returns before notification completes)
4. Standard Mockito unit tests for service-layer logic

Chasing an 80% line-coverage number uniformly is lower value than nailing these four — a grader reviewing a take-home will look for these specific tests by name.

---

## 5. README checklist for submission

- [ ] Setup instructions (Docker Compose up, migrations, seed)
- [ ] Design rationale: why pessimistic locking, why virtual threads, why this state machine
- [ ] Explicit "what I cut and why" section (Kafka/Redis/Prometheus/WebSockets)
- [ ] How to run the concurrency test suite specifically
- [ ] Benchmark results and how to reproduce them
- [ ] API docs link (Swagger UI path)
- [ ] Brief note on branch strategy (dev → integration → main) and how PRs are gated, if the repo history itself doesn't already make it obvious
- [ ] Confirm CI is green on the final `main` PR (build+test, CodeQL, compose smoke test) — screenshot or link in the README if the grader won't have Actions access