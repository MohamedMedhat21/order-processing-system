package io.github.mohamedmedhat21.order_processing_system.controller;

import io.github.mohamedmedhat21.order_processing_system.dto.auth.RegisterUserRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.auth.UpdateUserRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.auth.UserResponse;
import io.github.mohamedmedhat21.order_processing_system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {

	private final AuthService authService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register a new customer account")
	public UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
		return authService.register(request);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get user profile", security = @SecurityRequirement(name = "bearerAuth"))
	public UserResponse getUser(@PathVariable Long id) {
		return authService.getUser(id);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update user profile", security = @SecurityRequirement(name = "bearerAuth"))
	public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
		return authService.updateUser(id, request);
	}
}
