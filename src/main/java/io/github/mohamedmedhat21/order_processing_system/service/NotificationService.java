package io.github.mohamedmedhat21.order_processing_system.service;

import java.time.Instant;

import io.github.mohamedmedhat21.order_processing_system.config.RabbitMqConfig;
import io.github.mohamedmedhat21.order_processing_system.dto.messaging.OrderNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

	private final RabbitTemplate rabbitTemplate;

	public void publishOrderNotification(Long orderId) {
		OrderNotificationEvent event = new OrderNotificationEvent(orderId, Instant.now());
		rabbitTemplate.convertAndSend(
				RabbitMqConfig.ORDER_NOTIFICATION_EXCHANGE,
				RabbitMqConfig.ORDER_NOTIFICATION_ROUTING_KEY,
				event
		);
		log.debug("Published order notification event for order {}", orderId);
	}
}
