package io.github.mohamedmedhat21.order_processing_system.dto.order;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record CreateOrderRequest(
		@NotEmpty List<@Valid OrderLineRequest> items
) {
}
