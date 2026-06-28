package io.github.mohamedmedhat21.order_processing_system.config;

import io.github.mohamedmedhat21.order_processing_system.dto.ErrorResponse;
import io.github.mohamedmedhat21.order_processing_system.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, RateLimitProperties.class})
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final RateLimitProperties rateLimitProperties;
	private final JsonMapper jsonMapper;

	@Bean
	OrderCreationRateLimitFilter orderCreationRateLimitFilter() {
		return new OrderCreationRateLimitFilter(rateLimitProperties, jsonMapper);
	}

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			OrderCreationRateLimitFilter orderCreationRateLimitFilter
	) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/actuator/health",
								"/actuator/health/**",
								"/api/v1/auth/login",
								"/api/v1/auth/refresh",
								"/api/v1/auth/logout",
								"/api/v1/users",
								"/api/v1/products",
								"/api/v1/products/**",
								"/swagger-ui/**",
								"/v3/api-docs/**"
						).permitAll()
						.anyRequest().authenticated()
				)
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(this::writeUnauthorized)
						.accessDeniedHandler(this::writeForbidden)
				)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterAfter(orderCreationRateLimitFilter, JwtAuthenticationFilter.class)
				.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response, Exception ex)
			throws java.io.IOException {
		writeError(response, request, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
	}

	private void writeForbidden(HttpServletRequest request, HttpServletResponse response,
			org.springframework.security.access.AccessDeniedException ex) throws java.io.IOException {
		writeError(response, request, HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied");
	}

	private void writeError(
			HttpServletResponse response,
			HttpServletRequest request,
			HttpStatus status,
			String code,
			String message
	) throws java.io.IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		jsonMapper.writeValue(response.getOutputStream(),
				ErrorResponse.of(status.value(), code, message, request.getRequestURI()));
	}
}
