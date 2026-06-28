package io.github.mohamedmedhat21.order_processing_system;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.mohamedmedhat21.order_processing_system.domain.Inventory;
import io.github.mohamedmedhat21.order_processing_system.dto.order.CreateOrderRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.order.OrderLineRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.order.OrderResponse;
import io.github.mohamedmedhat21.order_processing_system.exception.InsufficientInventoryException;
import io.github.mohamedmedhat21.order_processing_system.repository.InventoryRepository;
import io.github.mohamedmedhat21.order_processing_system.repository.OrderRepository;
import io.github.mohamedmedhat21.order_processing_system.service.OrderService;
import io.github.mohamedmedhat21.order_processing_system.statemachine.OrderStatus;
import io.github.mohamedmedhat21.order_processing_system.support.SecurityTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@TestPropertySource(properties = {
		"app.rate-limit.order-creation.capacity=1000",
		"app.rate-limit.order-creation.refill-tokens=1000"
})
class InventoryConcurrencyTest {

	private static final Long WEBCAM_PRODUCT_ID = 5L;
	private static final int CONCURRENT_BUYERS = 50;

	@Autowired
	private OrderService orderService;

	@Autowired
	private InventoryRepository inventoryRepository;

	@Autowired
	private OrderRepository orderRepository;

	@BeforeEach
	void resetLastUnitInventory() {
		Inventory inventory = inventoryRepository.findByProductId(WEBCAM_PRODUCT_ID).orElseThrow();
		inventory.setQuantityAvailable(1);
		inventoryRepository.save(inventory);
	}

	@Test
	void onlyOneConcurrentBuyerSucceedsForTheLastUnit() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_BUYERS);
		CountDownLatch startGate = new CountDownLatch(1);
		CountDownLatch doneGate = new CountDownLatch(CONCURRENT_BUYERS);
		AtomicInteger successes = new AtomicInteger();
		AtomicInteger inventoryFailures = new AtomicInteger();

		CreateOrderRequest request = new CreateOrderRequest(
				List.of(new OrderLineRequest(WEBCAM_PRODUCT_ID, 1))
		);

		for (int i = 0; i < CONCURRENT_BUYERS; i++) {
			executor.submit(() -> {
				try {
					startGate.await();
					SecurityTestSupport.runAs(SecurityTestSupport.customerPrincipal(), () -> {
						try {
							OrderResponse response = orderService.createOrder(request);
							if (response.status() == OrderStatus.FULFILLED) {
								successes.incrementAndGet();
							}
						}
						catch (InsufficientInventoryException ex) {
							inventoryFailures.incrementAndGet();
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

		startGate.countDown();
		assertThat(doneGate.await(120, TimeUnit.SECONDS)).isTrue();
		executor.shutdown();

		assertThat(successes.get()).isEqualTo(1);
		assertThat(inventoryFailures.get()).isEqualTo(CONCURRENT_BUYERS - 1);
		assertThat(inventoryRepository.findByProductId(WEBCAM_PRODUCT_ID).orElseThrow().getQuantityAvailable())
				.isZero();
		assertThat(orderRepository.findAll().stream()
				.filter(order -> order.getStatus() == OrderStatus.FULFILLED
						|| order.getStatus() == OrderStatus.NOTIFIED)
				.count()).isEqualTo(1);
	}
}
