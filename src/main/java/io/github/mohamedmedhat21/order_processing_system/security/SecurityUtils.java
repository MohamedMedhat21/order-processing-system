package io.github.mohamedmedhat21.order_processing_system.security;

import io.github.mohamedmedhat21.order_processing_system.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

	private SecurityUtils() {
	}

	public static UserPrincipal currentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
			throw new UnauthorizedException("Authentication required");
		}
		return principal;
	}
}
