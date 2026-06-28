package io.github.mohamedmedhat21.order_processing_system.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank @Email String email,
		@NotBlank String password
) {
}
