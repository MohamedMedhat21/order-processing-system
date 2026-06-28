package io.github.mohamedmedhat21.order_processing_system.security;

import java.util.Date;

import javax.crypto.SecretKey;

import io.github.mohamedmedhat21.order_processing_system.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final SecretKey signingKey;
	private final long expirationMs;

	public JwtService(JwtProperties jwtProperties) {
		this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
		this.expirationMs = jwtProperties.expirationMs();
	}

	public String generateToken(UserPrincipal principal) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + expirationMs);
		return Jwts.builder()
				.subject(principal.email())
				.claim("userId", principal.id())
				.claim("role", principal.role().name())
				.issuedAt(now)
				.expiration(expiry)
				.signWith(signingKey)
				.compact();
	}

	public UserPrincipal parseToken(String token) {
		Claims claims = Jwts.parser()
				.verifyWith(signingKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();

		return new UserPrincipal(
				claims.get("userId", Long.class),
				claims.getSubject(),
				"",
				io.github.mohamedmedhat21.order_processing_system.domain.UserRole.valueOf(claims.get("role", String.class))
		);
	}

	public long getExpirationMs() {
		return expirationMs;
	}
}
