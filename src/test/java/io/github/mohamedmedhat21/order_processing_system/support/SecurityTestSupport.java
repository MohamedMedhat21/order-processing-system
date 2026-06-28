package io.github.mohamedmedhat21.order_processing_system.support;

import io.github.mohamedmedhat21.order_processing_system.domain.UserRole;
import io.github.mohamedmedhat21.order_processing_system.security.UserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityTestSupport {

	private SecurityTestSupport() {
	}

	public static UserPrincipal customerPrincipal() {
		return new UserPrincipal(2L, "customer@example.com", "hash", UserRole.CUSTOMER);
	}

	public static UserPrincipal adminPrincipal() {
		return new UserPrincipal(1L, "admin@example.com", "hash", UserRole.ADMIN);
	}

	public static void runAs(UserPrincipal principal, Runnable action) {
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(new UsernamePasswordAuthenticationToken(
				principal,
				null,
				principal.getAuthorities()
		));
		SecurityContextHolder.setContext(context);
		try {
			action.run();
		}
		finally {
			SecurityContextHolder.clearContext();
		}
	}

	public static <T> T callAs(UserPrincipal principal, java.util.concurrent.Callable<T> action) throws Exception {
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(new UsernamePasswordAuthenticationToken(
				principal,
				null,
				principal.getAuthorities()
		));
		SecurityContextHolder.setContext(context);
		try {
			return action.call();
		}
		finally {
			SecurityContextHolder.clearContext();
		}
	}
}
