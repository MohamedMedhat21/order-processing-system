package io.github.mohamedmedhat21.order_processing_system.dto.product;

import io.github.mohamedmedhat21.order_processing_system.domain.Product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
		Long id,
		String name,
		String description,
		BigDecimal price,
		boolean active,
		Instant createdAt,
		Instant updatedAt
) {

	public static ProductResponse from(Product product) {
		return new ProductResponse(
				product.getId(),
				product.getName(),
				product.getDescription(),
				product.getPrice(),
				product.isActive(),
				product.getCreatedAt(),
				product.getUpdatedAt()
		);
	}
}
