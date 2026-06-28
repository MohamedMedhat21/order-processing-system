package io.github.mohamedmedhat21.order_processing_system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;

import io.github.mohamedmedhat21.order_processing_system.config.SlowNotificationTestConfig;
import io.github.mohamedmedhat21.order_processing_system.domain.Inventory;
import io.github.mohamedmedhat21.order_processing_system.dto.order.CreateOrderRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.order.OrderLineRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.order.OrderResponse;
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

@Import({TestcontainersConfiguration.class, SlowNotificationTestConfig.class})
@SpringBootTest
@TestPropertySource(properties = {
		"app.rate-limit.order-creation.capacity=1000",
		"app.rate-limit.order-creation.refill-tokens=1000"
})
class AsyncNonBlockingConcurrencyTest {

	private static final Long PRODUCT_ID = 1L;

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private InventoryRepository inventoryRepository;

	@BeforeEach
	void ensureInventoryAvailable() {
		Inventory inventory = inventoryRepository.findByProductId(PRODUCT_ID).orElseThrow();
		if (inventory.getQuantityAvailable() < 10) {
			inventory.setQuantityAvailable(100);
			inventoryRepository.save(inventory);
		}
	}

	@Test
	void orderCreationReturnsBeforeNotificationCompletes() throws Exception {
		CreateOrderRequest request = new CreateOrderRequest(
				List.of(new OrderLineRequest(PRODUCT_ID, 1))
		);

		long startNanos = System.nanoTime();
		OrderResponse response = SecurityTestSupport.callAs(
				SecurityTestSupport.customerPrincipal(),
				() -> orderService.createOrder(request)
		);
		long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

		assertThat(response.status()).isEqualTo(OrderStatus.FULFILLED);
		assertThat(elapsedMs).isLessThan(1_000);

		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
			OrderStatus status = orderRepository.findById(response.id()).orElseThrow().getStatus();
			assertThat(status).isEqualTo(OrderStatus.NOTIFIED);
		});
	}
}
