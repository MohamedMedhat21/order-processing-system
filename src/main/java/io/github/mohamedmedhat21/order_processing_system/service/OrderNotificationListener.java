package io.github.mohamedmedhat21.order_processing_system.service;

import io.github.mohamedmedhat21.order_processing_system.config.RabbitMqConfig;
import io.github.mohamedmedhat21.order_processing_system.dto.messaging.OrderNotificationEvent;
import io.github.mohamedmedhat21.order_processing_system.exception.InvalidStateTransitionException;
import io.github.mohamedmedhat21.order_processing_system.repository.OrderRepository;
import io.github.mohamedmedhat21.order_processing_system.statemachine.OrderStateMachine;
import io.github.mohamedmedhat21.order_processing_system.statemachine.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationListener {

	private final OrderStateMachine orderStateMachine;
	private final OrderRepository orderRepository;

	@Value("${app.notifications.processing-delay-ms:0}")
	private long processingDelayMs;

	@RabbitListener(queues = RabbitMqConfig.ORDER_NOTIFICATION_QUEUE)
	public void handleOrderNotification(OrderNotificationEvent event) {
		processNotification(event.orderId());
	}

	void processNotification(Long orderId) {
		if (isAlreadyNotified(orderId)) {
			log.debug("Order {} already notified — skipping duplicate message", orderId);
			return;
		}

		applyProcessingDelay();

		try {
			orderStateMachine.notifyCustomer(orderId);
		}
		catch (InvalidStateTransitionException ex) {
			if (isAlreadyNotified(orderId)) {
				log.debug("Order {} notified concurrently — ignoring duplicate delivery", orderId);
				return;
			}
			throw ex;
		}
	}

	private boolean isAlreadyNotified(Long orderId) {
		return orderRepository.findById(orderId)
				.map(order -> order.getStatus() == OrderStatus.NOTIFIED)
				.orElse(false);
	}

	private void applyProcessingDelay() {
		if (processingDelayMs <= 0) {
			return;
		}
		try {
			Thread.sleep(processingDelayMs);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}
}
