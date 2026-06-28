package io.github.mohamedmedhat21.order_processing_system.dto.order;

import java.math.BigDecimal;

public record OrderItemResponse(
		Long productId,
		String productName,
		int quantity,
		BigDecimal unitPrice
) {
}
