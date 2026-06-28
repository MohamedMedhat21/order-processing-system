package io.github.mohamedmedhat21.order_processing_system.dto.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
		@NotBlank @Size(max = 255) String name,
		@Size(max = 2000) String description,
		@NotNull @DecimalMin("0.00") BigDecimal price,
		@NotNull @DecimalMin("0") Integer initialQuantity
) {
}
