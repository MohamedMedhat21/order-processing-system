---
name: concurrency-test-patterns
description: Patterns for writing concurrency tests in this Java/Spring Boot order-processing project — race-condition tests for inventory locking, payment idempotency tests, async/non-blocking assertions for notifications, and concurrent order state-transition tests. Use this whenever writing or reviewing a test for the inventory reservation, payment processing, order state machine, or notification flow, or whenever asked to verify "no race condition," "no double charge," "no overselling," "no invalid state transition," or "doesn't block." Also use when a generic test for these flows seems flaky or non-deterministic — these patterns exist specifically to avoid that.
---

# Concurrency Test Patterns

This project's grading hinges on three specific concurrency guarantees. Each has a known-good test shape below. Follow these rather than improvising a different structure — generic unit tests don't actually prove the guarantee a reviewer is looking for.

## 1. Race-condition test (inventory)

**Goal:** prove that under N simultaneous purchase attempts for the last unit(s) of stock, exactly the correct number succeed and the rest fail cleanly — no overselling, no deadlock, no silent double-decrement.

**Required setup:**
- Use **Testcontainers** with a real PostgreSQL instance. Never use H2 for this test — H2's locking semantics don't reliably reproduce row-lock contention, and a test that passes against H2 proves nothing about the actual guarantee.
- Seed exactly 1 unit of inventory for a product.
- Launch N (e.g. 50) concurrent attempts using `Executors.newVirtualThreadPerTaskExecutor()`, synchronized to start together with a `CountDownLatch` so they actually contend rather than running sequentially.
- Track successes with an `AtomicInteger`; assert on the exact count, not just "more than one failed."
- Assert the final inventory count in the DB is exactly 0, never negative.

```java
@SpringBootTest
@Testcontainers
class InventoryConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @Autowired OrderService orderService;
    @Autowired InventoryRepository inventoryRepository;

    @Test
    void onlyOneOrderSucceedsForLastUnit() throws InterruptedException {
        int threads = 50;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        orderService.placeOrder(testOrderRequest());
                        successCount.incrementAndGet();
                    } catch (InsufficientInventoryException expected) {
                        // expected for the losing threads — swallow
                    } catch (Exception e) {
                        fail("Unexpected exception: " + e);
                    }
                });
            }
            ready.await();
            start.countDown();
        }

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(inventoryRepository.findByProductId(productId).getQuantity()).isZero();
    }
}
```

## 2. Idempotency test (payment)

**Goal:** prove that retrying the same logical payment (e.g. a client retry after a timeout) never results in two charges.

**Shape:**
- Call the payment path twice with the **same idempotency key** — write the concurrent version if time allows, since it's the more convincing proof than a sequential retry.
- Assert exactly one `Payment` row exists for that key afterward.
- Assert the second call returns the original/cached result rather than creating a new charge.

```java
@Test
void duplicateRequestWithSameIdempotencyKeyChargesOnce() throws InterruptedException {
    String idempotencyKey = orderId.toString();
    CountDownLatch start = new CountDownLatch(1);

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        var futures = IntStream.range(0, 2).mapToObj(i -> executor.submit(() -> {
            start.await();
            return paymentService.processPayment(idempotencyKey, paymentRequest());
        })).toList();
        start.countDown();
        for (var f : futures) f.get();
    }

    assertThat(paymentRepository.countByIdempotencyKey(idempotencyKey)).isEqualTo(1);
}
```

## 3. Non-blocking async test (notifications)

**Goal:** prove order processing doesn't wait on notification delivery.

**Shape:**
- Mock or artificially delay the notification sender (e.g. `Thread.sleep` in a test double) to a duration clearly longer than a reasonable HTTP timeout (e.g. 3s).
- Call the order-placement endpoint and assert the response returns in well under that duration (e.g. < 500ms).
- Separately, use `Awaitility` to confirm the notification *eventually* gets sent — don't just assert it's missing immediately, that doesn't distinguish "async and pending" from "never sent."

```java
@Test
void orderResponseDoesNotWaitOnSlowNotification() {
    long start = System.currentTimeMillis();
    ResponseEntity<OrderResponse> response =
        restTemplate.postForEntity("/api/v1/orders", request, OrderResponse.class);
    long elapsed = System.currentTimeMillis() - start;

    assertThat(elapsed).isLessThan(500);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    await().atMost(5, SECONDS)
        .until(() -> notificationRepository.existsByOrderId(response.getBody().orderId()));
}
```

## 4. Concurrent state-transition test (order state machine)

**Goal:** prove that two concurrent attempts to transition the *same* order (e.g. an admin status update racing a fulfillment job) can't both succeed if they'd produce an inconsistent result — the row lock used for inventory reservation should also protect the order's own status field.

**Shape:**
- Seed one order in a known state (e.g. `INVENTORY_RESERVED`).
- Launch two concurrent transition attempts that both assume that starting state (e.g. one tries to move it to `PAYMENT_PROCESSING`, the other to `CANCELLED`).
- Assert exactly one succeeds and the other receives a clean `InvalidOrderStateTransitionException` (or a stale-state failure) — never both succeeding, and never a corrupted intermediate state.
- Assert the final persisted status is one of the two valid outcomes, not something outside the transition table.

This pattern is structurally identical to the inventory race-condition test (latch-synchronized virtual threads + real Postgres) — reuse the same harness, just point it at a state transition instead of an inventory decrement.

## When NOT to use these patterns

- Plain unit tests of service logic with Mockito don't need any of this — reserve these patterns for tests that specifically assert a concurrency / race / idempotency / non-blocking guarantee.
- Don't add `Thread.sleep` to *production* code to paper over a race — these patterns are for tests, never a substitute for fixing the underlying locking or transaction boundary.
- If a test using these patterns is still flaky, the fix is almost always a transaction-boundary or isolation-level issue in the production code, not a longer sleep or a higher thread count in the test.
