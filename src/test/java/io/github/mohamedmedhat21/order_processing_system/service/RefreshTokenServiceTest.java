package io.github.mohamedmedhat21.order_processing_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import io.github.mohamedmedhat21.order_processing_system.config.JwtProperties;
import io.github.mohamedmedhat21.order_processing_system.domain.RefreshToken;
import io.github.mohamedmedhat21.order_processing_system.domain.User;
import io.github.mohamedmedhat21.order_processing_system.domain.UserRole;
import io.github.mohamedmedhat21.order_processing_system.exception.UnauthorizedException;
import io.github.mohamedmedhat21.order_processing_system.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

	private static final JwtProperties JWT_PROPERTIES =
			new JwtProperties("bW9oYW1lZC1kZXYtand0LXNlY3JldC0zMi1ieXRlcyEh", 3_600_000, 604_800_000);

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	private RefreshTokenService refreshTokenService;

	@BeforeEach
	void setUp() {
		refreshTokenService = new RefreshTokenService(refreshTokenRepository, JWT_PROPERTIES);
	}

	@Test
	void issueForUserPersistsHashedToken() {
		User user = user(1L);
		when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

		RefreshTokenService.IssuedRefreshToken issued = refreshTokenService.issueForUser(user);

		assertThat(issued.token()).isNotBlank();
		assertThat(issued.expiresInMs()).isEqualTo(604_800_000);

		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(captor.capture());
		RefreshToken saved = captor.getValue();
		assertThat(saved.getTokenHash()).isEqualTo(RefreshTokenService.hashToken(issued.token()));
		assertThat(saved.getTokenHash()).isNotEqualTo(issued.token());
		assertThat(saved.isRevoked()).isFalse();
	}

	@Test
	void validateAndRevokeRejectsUnknownToken() {
		when(refreshTokenRepository.findByTokenHashAndRevokedFalseAndExpiresAtAfter(any(), any()))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> refreshTokenService.validateAndRevoke("unknown-token"))
				.isInstanceOf(UnauthorizedException.class)
				.hasMessageContaining("Invalid or expired refresh token");
	}

	@Test
	void validateAndRevokeMarksTokenRevoked() {
		User user = user(7L);
		RefreshToken stored = new RefreshToken();
		stored.setId(99L);
		stored.setUser(user);
		stored.setTokenHash(RefreshTokenService.hashToken("valid-token"));
		stored.setExpiresAt(Instant.now().plusSeconds(3600));
		stored.setRevoked(false);

		when(refreshTokenRepository.findByTokenHashAndRevokedFalseAndExpiresAtAfter(
				eq(stored.getTokenHash()), any()))
				.thenReturn(Optional.of(stored));
		when(refreshTokenRepository.save(stored)).thenReturn(stored);

		User result = refreshTokenService.validateAndRevoke("valid-token");

		assertThat(result).isSameAs(user);
		assertThat(stored.isRevoked()).isTrue();
	}

	private static User user(Long id) {
		User user = new User();
		user.setId(id);
		user.setEmail("user@example.com");
		user.setFullName("User");
		user.setPasswordHash("hash");
		user.setRole(UserRole.CUSTOMER);
		return user;
	}
}
