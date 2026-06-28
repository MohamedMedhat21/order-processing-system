package io.github.mohamedmedhat21.order_processing_system.dto.messaging;

import java.time.Instant;

public record OrderNotificationEvent(Long orderId, Instant occurredAt) {
}
