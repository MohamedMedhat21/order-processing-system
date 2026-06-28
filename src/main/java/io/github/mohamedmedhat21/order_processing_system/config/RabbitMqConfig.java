package io.github.mohamedmedhat21.order_processing_system.config;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMqConfig {

	public static final String ORDER_NOTIFICATION_EXCHANGE = "order.notifications";
	public static final String ORDER_NOTIFICATION_QUEUE = "order.notifications.queue";
	public static final String ORDER_NOTIFICATION_DLQ = "order.notifications.dlq";
	public static final String ORDER_NOTIFICATION_ROUTING_KEY = "order.notification";

	@Bean
	DirectExchange orderNotificationExchange() {
		return new DirectExchange(ORDER_NOTIFICATION_EXCHANGE, true, false);
	}

	@Bean
	Queue orderNotificationDeadLetterQueue() {
		return QueueBuilder.durable(ORDER_NOTIFICATION_DLQ).build();
	}

	@Bean
	Queue orderNotificationQueue() {
		return QueueBuilder.durable(ORDER_NOTIFICATION_QUEUE)
				.withArgument("x-dead-letter-exchange", "")
				.withArgument("x-dead-letter-routing-key", ORDER_NOTIFICATION_DLQ)
				.build();
	}

	@Bean
	Binding orderNotificationBinding(
			Queue orderNotificationQueue,
			DirectExchange orderNotificationExchange
	) {
		return BindingBuilder.bind(orderNotificationQueue)
				.to(orderNotificationExchange)
				.with(ORDER_NOTIFICATION_ROUTING_KEY);
	}

	@Bean
	MessageConverter rabbitMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}
}
