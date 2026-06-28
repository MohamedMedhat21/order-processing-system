package io.github.mohamedmedhat21.order_processing_system;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import io.github.mohamedmedhat21.order_processing_system.domain.Inventory;
import io.github.mohamedmedhat21.order_processing_system.dto.order.CreateOrderRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.order.OrderLineRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.order.OrderResponse;
import io.github.mohamedmedhat21.order_processing_system.repository.InventoryRepository;
import io.github.mohamedmedhat21.order_processing_system.service.OrderService;
import io.github.mohamedmedhat21.order_processing_system.statemachine.OrderStatus;
import io.github.mohamedmedhat21.order_processing_system.support.SecurityTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Optional performance benchmark — excluded from default {@code mvn verify}.
 * Run explicitly: {@code ./mvnw test -Dgroups=benchmark}
 */
@Tag("benchmark")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@TestPropertySource(properties = {
		"app.rate-limit.order-creation.capacity=1000",
		"app.rate-limit.order-creation.refill-tokens=1000",
		"spring.datasource.hikari.maximum-pool-size=60"
})
class OrderSubmissionBenchmarkTest {

	private static final Logger log = LoggerFactory.getLogger(OrderSubmissionBenchmarkTest.class);

	private static final Long PRODUCT_ID = 1L;
	private static final int CONCURRENT_SUBMISSIONS = 30;

	@Autowired
	private OrderService orderService;

	@Autowired
	private InventoryRepository inventoryRepository;

	@BeforeEach
	void ensureInventoryHeadroom() {
		Inventory inventory = inventoryRepository.findByProductId(PRODUCT_ID).orElseThrow();
		inventory.setQuantityAvailable(200);
		inventoryRepository.save(inventory);
	}

	@Test
	void benchmarkConcurrentOrderSubmissions() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_SUBMISSIONS);
		CountDownLatch startGate = new CountDownLatch(1);
		CountDownLatch doneGate = new CountDownLatch(CONCURRENT_SUBMISSIONS);
		AtomicInteger successes = new AtomicInteger();
		AtomicLong totalLatencyNanos = new AtomicLong();

		CreateOrderRequest request = new CreateOrderRequest(
				List.of(new OrderLineRequest(PRODUCT_ID, 1))
		);

		for (int i = 0; i < CONCURRENT_SUBMISSIONS; i++) {
			executor.submit(() -> {
				try {
					startGate.await();
					long start = System.nanoTime();
					SecurityTestSupport.runAs(SecurityTestSupport.customerPrincipal(), () -> {
						OrderResponse response = orderService.createOrder(request);
						if (response.status() == OrderStatus.FULFILLED) {
							successes.incrementAndGet();
							totalLatencyNanos.addAndGet(System.nanoTime() - start);
						}
					});
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				finally {
					doneGate.countDown();
				}
			});
		}

		long wallStart = System.nanoTime();
		startGate.countDown();
		assertThat(doneGate.await(180, TimeUnit.SECONDS)).isTrue();
		long wallElapsedMs = (System.nanoTime() - wallStart) / 1_000_000;
		executor.shutdown();

		int successCount = successes.get();
		double avgLatencyMs = successCount == 0
				? 0
				: (totalLatencyNanos.get() / (double) successCount) / 1_000_000;
		double throughput = wallElapsedMs == 0
				? 0
				: successCount * 1000.0 / wallElapsedMs;

		log.info(
				"Benchmark: {} concurrent order submissions — wall {} ms, {} succeeded, avg per-order {} ms, ~{}/s throughput",
				CONCURRENT_SUBMISSIONS,
				wallElapsedMs,
				successCount,
				String.format("%.1f", avgLatencyMs),
				String.format("%.1f", throughput)
		);

		assertThat(successCount)
				.as("benchmark expects high success rate under concurrent load")
				.isGreaterThanOrEqualTo((int) (CONCURRENT_SUBMISSIONS * 0.9));
	}
}
