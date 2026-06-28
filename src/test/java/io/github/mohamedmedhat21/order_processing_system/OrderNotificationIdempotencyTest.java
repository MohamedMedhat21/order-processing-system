package io.github.mohamedmedhat21.order_processing_system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;

import io.github.mohamedmedhat21.order_processing_system.dto.order.CreateOrderRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.order.OrderLineRequest;
import io.github.mohamedmedhat21.order_processing_system.repository.NotificationRepository;
import io.github.mohamedmedhat21.order_processing_system.repository.OrderRepository;
import io.github.mohamedmedhat21.order_processing_system.service.NotificationService;
import io.github.mohamedmedhat21.order_processing_system.service.OrderDraftService;
import io.github.mohamedmedhat21.order_processing_system.service.OrderNotificationListener;
import io.github.mohamedmedhat21.order_processing_system.statemachine.OrderStateMachine;
import io.github.mohamedmedhat21.order_processing_system.statemachine.OrderStatus;
import io.github.mohamedmedhat21.order_processing_system.support.SecurityTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OrderNotificationIdempotencyTest {

	private static final Long PRODUCT_ID = 1L;

	@Autowired
	private OrderDraftService orderDraftService;

	@Autowired
	private OrderStateMachine orderStateMachine;

	@Autowired
	private OrderNotificationListener orderNotificationListener;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@Test
	void duplicateNotificationDeliveryNotifiesExactlyOnce() throws Exception {
		Long orderId = SecurityTestSupport.callAs(SecurityTestSupport.customerPrincipal(), () -> {
			Long id = orderDraftService.createDraft(new CreateOrderRequest(
					List.of(new OrderLineRequest(PRODUCT_ID, 1))
			)).getId();
			orderStateMachine.reserveInventory(id);
			orderStateMachine.beginPaymentProcessing(id);
			orderStateMachine.completePayment(id);
			orderStateMachine.fulfill(id);
			return id;
		});

		assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.FULFILLED);

		orderNotificationListener.handleOrderNotification(
				new io.github.mohamedmedhat21.order_processing_system.dto.messaging.OrderNotificationEvent(
						orderId,
						java.time.Instant.now()
				)
		);
		orderNotificationListener.handleOrderNotification(
				new io.github.mohamedmedhat21.order_processing_system.dto.messaging.OrderNotificationEvent(
						orderId,
						java.time.Instant.now()
				)
		);

		assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.NOTIFIED);
		assertThat(notificationRepository.findAll().stream()
				.filter(notification -> notification.getOrder().getId().equals(orderId))
				.count()).isEqualTo(1);
	}

	@Test
	void duplicateBrokerMessagesNotifiesExactlyOnce() throws Exception {
		Long orderId = SecurityTestSupport.callAs(SecurityTestSupport.customerPrincipal(), () -> {
			Long id = orderDraftService.createDraft(new CreateOrderRequest(
					List.of(new OrderLineRequest(PRODUCT_ID, 1))
			)).getId();
			orderStateMachine.reserveInventory(id);
			orderStateMachine.beginPaymentProcessing(id);
			orderStateMachine.completePayment(id);
			orderStateMachine.fulfill(id);
			return id;
		});

		notificationService.publishOrderNotification(orderId);
		notificationService.publishOrderNotification(orderId);

		await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
				assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.NOTIFIED)
		);

		assertThat(notificationRepository.findAll().stream()
				.filter(notification -> notification.getOrder().getId().equals(orderId))
				.count()).isEqualTo(1);
	}
}
