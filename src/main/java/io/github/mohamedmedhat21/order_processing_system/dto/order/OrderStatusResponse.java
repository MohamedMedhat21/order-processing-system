package io.github.mohamedmedhat21.order_processing_system.dto.order;

import io.github.mohamedmedhat21.order_processing_system.statemachine.OrderStatus;

import java.time.Instant;

public record OrderStatusResponse(Long orderId, OrderStatus status, Instant updatedAt) {
}
