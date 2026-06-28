package io.github.mohamedmedhat21.order_processing_system.controller;

import io.github.mohamedmedhat21.order_processing_system.dto.auth.LoginRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.auth.LoginResponse;
import io.github.mohamedmedhat21.order_processing_system.dto.auth.RefreshTokenRequest;
import io.github.mohamedmedhat21.order_processing_system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	@Operation(summary = "Authenticate and receive access + refresh tokens")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/refresh")
	@Operation(summary = "Exchange a valid refresh token for a new access + refresh token pair (rotation)")
	public LoginResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return authService.refresh(request);
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Revoke the provided refresh token")
	public void logout(@Valid @RequestBody RefreshTokenRequest request) {
		authService.logout(request);
	}
}
