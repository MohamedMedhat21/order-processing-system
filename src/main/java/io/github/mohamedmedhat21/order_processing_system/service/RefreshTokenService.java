package io.github.mohamedmedhat21.order_processing_system.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import io.github.mohamedmedhat21.order_processing_system.config.JwtProperties;
import io.github.mohamedmedhat21.order_processing_system.domain.RefreshToken;
import io.github.mohamedmedhat21.order_processing_system.domain.User;
import io.github.mohamedmedhat21.order_processing_system.exception.UnauthorizedException;
import io.github.mohamedmedhat21.order_processing_system.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtProperties jwtProperties;

	@Transactional
	public IssuedRefreshToken issueForUser(User user) {
		String rawToken = generateRawToken();
		RefreshToken entity = new RefreshToken();
		entity.setUser(user);
		entity.setTokenHash(hashToken(rawToken));
		entity.setExpiresAt(Instant.now().plusMillis(jwtProperties.refreshExpirationMs()));
		entity.setRevoked(false);
		refreshTokenRepository.save(entity);
		return new IssuedRefreshToken(rawToken, jwtProperties.refreshExpirationMs());
	}

	@Transactional
	public User validateAndRevoke(String rawToken) {
		RefreshToken stored = refreshTokenRepository
				.findByTokenHashAndRevokedFalseAndExpiresAtAfter(hashToken(rawToken), Instant.now())
				.orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

		stored.setRevoked(true);
		refreshTokenRepository.save(stored);
		return stored.getUser();
	}

	@Transactional
	public void revokeAllForUser(Long userId) {
		refreshTokenRepository.revokeAllActiveForUser(userId);
	}

	private static String generateRawToken() {
		byte[] bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	static String hashToken(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 not available", ex);
		}
	}

	public record IssuedRefreshToken(String token, long expiresInMs) {
	}
}
