package io.github.mohamedmedhat21.order_processing_system.dto.auth;

import io.github.mohamedmedhat21.order_processing_system.domain.User;
import io.github.mohamedmedhat21.order_processing_system.domain.UserRole;

import java.time.Instant;

public record UserResponse(
		Long id,
		String email,
		String fullName,
		UserRole role,
		Instant createdAt,
		Instant updatedAt
) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getFullName(),
				user.getRole(),
				user.getCreatedAt(),
				user.getUpdatedAt()
		);
	}
}
