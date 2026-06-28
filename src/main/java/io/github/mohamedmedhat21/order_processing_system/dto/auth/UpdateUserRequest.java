package io.github.mohamedmedhat21.order_processing_system.dto.auth;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
		@Size(max = 255) String fullName,
		@Size(min = 8, max = 100) String password
) {
}
