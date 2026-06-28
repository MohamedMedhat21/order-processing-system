package io.github.mohamedmedhat21.order_processing_system.config;

import io.github.mohamedmedhat21.order_processing_system.service.NotificationService;
import io.github.mohamedmedhat21.order_processing_system.statemachine.OrderStateMachine;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;

@TestConfiguration
public class SlowNotificationTestConfig {

	@Bean
	@Primary
	NotificationService slowNotificationService(OrderStateMachine orderStateMachine) {
		return new NotificationService(orderStateMachine) {
			@Override
			@Async("virtualThreadExecutor")
			public void sendOrderNotificationAsync(Long orderId) {
				try {
					Thread.sleep(1_500);
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				orderStateMachine.notifyCustomer(orderId);
			}
		};
	}
}
