package io.github.mohamedmedhat21.order_processing_system.service;

import io.github.mohamedmedhat21.order_processing_system.domain.User;
import io.github.mohamedmedhat21.order_processing_system.domain.UserRole;
import io.github.mohamedmedhat21.order_processing_system.dto.auth.LoginRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.auth.LoginResponse;
import io.github.mohamedmedhat21.order_processing_system.dto.auth.RefreshTokenRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.auth.RegisterUserRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.auth.UpdateUserRequest;
import io.github.mohamedmedhat21.order_processing_system.dto.auth.UserResponse;
import io.github.mohamedmedhat21.order_processing_system.exception.ConflictException;
import io.github.mohamedmedhat21.order_processing_system.exception.ForbiddenException;
import io.github.mohamedmedhat21.order_processing_system.exception.ResourceNotFoundException;
import io.github.mohamedmedhat21.order_processing_system.exception.UnauthorizedException;
import io.github.mohamedmedhat21.order_processing_system.repository.UserRepository;
import io.github.mohamedmedhat21.order_processing_system.security.JwtService;
import io.github.mohamedmedhat21.order_processing_system.security.SecurityUtils;
import io.github.mohamedmedhat21.order_processing_system.security.UserPrincipal;
import io.github.mohamedmedhat21.order_processing_system.service.RefreshTokenService.IssuedRefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final RefreshTokenService refreshTokenService;

	@Transactional
	public UserResponse register(RegisterUserRequest request) {
		if (userRepository.findByEmail(request.email()).isPresent()) {
			throw new ConflictException("Email already registered: " + request.email());
		}

		User user = new User();
		user.setEmail(request.email());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setFullName(request.fullName());
		user.setRole(UserRole.CUSTOMER);
		return UserResponse.from(userRepository.save(user));
	}

	@Transactional
	public LoginResponse login(LoginRequest request) {
		User user = authenticate(request.email(), request.password());
		return issueTokenPair(user);
	}

	@Transactional
	public LoginResponse refresh(RefreshTokenRequest request) {
		User user = refreshTokenService.validateAndRevoke(request.refreshToken());
		return issueTokenPair(user);
	}

	@Transactional
	public void logout(RefreshTokenRequest request) {
		try {
			refreshTokenService.validateAndRevoke(request.refreshToken());
		}
		catch (UnauthorizedException ignored) {
			// Idempotent logout — invalid token is treated as already logged out.
		}
	}

	@Transactional(readOnly = true)
	public UserResponse getUser(Long id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User", id));
		assertSelfOrAdmin(user.getId());
		return UserResponse.from(user);
	}

	@Transactional
	public UserResponse updateUser(Long id, UpdateUserRequest request) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User", id));
		assertSelfOrAdmin(user.getId());

		boolean passwordChanged = request.password() != null && !request.password().isBlank();

		if (request.fullName() != null && !request.fullName().isBlank()) {
			user.setFullName(request.fullName());
		}
		if (passwordChanged) {
			user.setPasswordHash(passwordEncoder.encode(request.password()));
			refreshTokenService.revokeAllForUser(user.getId());
		}
		return UserResponse.from(userRepository.save(user));
	}

	private User authenticate(String email, String password) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			throw new UnauthorizedException("Invalid email or password");
		}
		return user;
	}

	private LoginResponse issueTokenPair(User user) {
		UserPrincipal principal = UserPrincipal.from(user);
		IssuedRefreshToken refreshToken = refreshTokenService.issueForUser(user);
		return new LoginResponse(
				jwtService.generateToken(principal),
				refreshToken.token(),
				jwtService.getExpirationMs(),
				refreshToken.expiresInMs()
		);
	}

	private void assertSelfOrAdmin(Long userId) {
		UserPrincipal current = SecurityUtils.currentUser();
		if (!current.id().equals(userId) && current.role() != UserRole.ADMIN) {
			throw new ForbiddenException("You can only access your own profile");
		}
	}
}
