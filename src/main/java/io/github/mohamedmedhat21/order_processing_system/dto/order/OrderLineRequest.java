package io.github.mohamedmedhat21.order_processing_system.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderLineRequest(
		@NotNull Long productId,
		@Min(1) int quantity
) {
}
