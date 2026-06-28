package io.github.mohamedmedhat21.order_processing_system.dto.admin;

public record LowStockAlertResponse(
		Long productId,
		String productName,
		int quantityAvailable,
		int threshold
) {
}
