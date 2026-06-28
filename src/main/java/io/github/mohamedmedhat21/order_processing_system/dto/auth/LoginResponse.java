package io.github.mohamedmedhat21.order_processing_system.dto.auth;

public record LoginResponse(
		String accessToken,
		String refreshToken,
		long accessExpiresInMs,
		long refreshExpiresInMs
) {
}
