package io.github.mohamedmedhat21.order_processing_system.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mohamedmedhat21.order_processing_system.config.JwtProperties;
import io.github.mohamedmedhat21.order_processing_system.domain.UserRole;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

	private static final String TEST_SECRET = "bW9oYW1lZC1kZXYtand0LXNlY3JldC0zMi1ieXRlcyEh";

	@Test
	void generatesAndParsesAccessToken() {
		JwtService jwtService = new JwtService(new JwtProperties(TEST_SECRET, 3_600_000, 604_800_000));
		UserPrincipal principal = new UserPrincipal(42L, "customer@example.com", "hash", UserRole.CUSTOMER);

		String token = jwtService.generateToken(principal);
		UserPrincipal parsed = jwtService.parseToken(token);

		assertThat(parsed.id()).isEqualTo(42L);
		assertThat(parsed.email()).isEqualTo("customer@example.com");
		assertThat(parsed.role()).isEqualTo(UserRole.CUSTOMER);
	}
}
