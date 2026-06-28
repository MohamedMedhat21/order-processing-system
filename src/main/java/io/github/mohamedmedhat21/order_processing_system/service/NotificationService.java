package io.github.mohamedmedhat21.order_processing_system.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import io.github.mohamedmedhat21.order_processing_system.statemachine.OrderStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

	private final OrderStateMachine orderStateMachine;

	@Async("virtualThreadExecutor")
	public void sendOrderNotificationAsync(Long orderId) {
		log.debug("Sending async notification for order {}", orderId);
		orderStateMachine.notifyCustomer(orderId);
	}
}
