package io.github.mohamedmedhat21.order_processing_system.exception;

import org.springframework.http.HttpStatus;

public class InsufficientInventoryException extends BaseBusinessException {

	public InsufficientInventoryException(Long productId, int requested, int available) {
		super(
				HttpStatus.CONFLICT,
				"INSUFFICIENT_INVENTORY",
				"Insufficient inventory for product %d: requested %d, available %d"
						.formatted(productId, requested, available)
		);
	}
}
