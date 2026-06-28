package io.github.mohamedmedhat21.order_processing_system.dto.order;

import io.github.mohamedmedhat21.order_processing_system.domain.Order;
import io.github.mohamedmedhat21.order_processing_system.statemachine.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
		Long id,
		Long userId,
		OrderStatus status,
		BigDecimal totalAmount,
		List<OrderItemResponse> items,
		Instant createdAt,
		Instant updatedAt
) {

	public static OrderResponse from(Order order) {
		List<OrderItemResponse> items = order.getItems().stream()
				.map(item -> new OrderItemResponse(
						item.getProduct().getId(),
						item.getProduct().getName(),
						item.getQuantity(),
						item.getUnitPrice()
				))
				.toList();

		return new OrderResponse(
				order.getId(),
				order.getUser().getId(),
				order.getStatus(),
				order.getTotalAmount(),
				items,
				order.getCreatedAt(),
				order.getUpdatedAt()
		);
	}
}
