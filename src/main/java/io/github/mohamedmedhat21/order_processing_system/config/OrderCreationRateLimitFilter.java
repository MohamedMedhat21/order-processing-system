package io.github.mohamedmedhat21.order_processing_system.config;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.mohamedmedhat21.order_processing_system.dto.ErrorResponse;
import io.github.mohamedmedhat21.order_processing_system.security.UserPrincipal;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

public class OrderCreationRateLimitFilter extends OncePerRequestFilter {

	private static final String ORDER_CREATE_PATH = "/api/v1/orders";

	private final Map<String, Bucket> bucketsByKey = new ConcurrentHashMap<>();
	private final RateLimitProperties rateLimitProperties;
	private final JsonMapper jsonMapper;

	public OrderCreationRateLimitFilter(RateLimitProperties rateLimitProperties, JsonMapper jsonMapper) {
		this.rateLimitProperties = rateLimitProperties;
		this.jsonMapper = jsonMapper;
	}

	@Override
	protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
		return !"POST".equalsIgnoreCase(request.getMethod())
				|| !ORDER_CREATE_PATH.equals(request.getRequestURI());
	}

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain
	) throws ServletException, IOException {
		Bucket bucket = bucketsByKey.computeIfAbsent(resolveBucketKey(request), key -> newBucket());

		if (!bucket.tryConsume(1)) {
			writeRateLimitResponse(request, response);
			return;
		}

		filterChain.doFilter(request, response);
	}

	private String resolveBucketKey(HttpServletRequest request) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
			return "user:" + principal.id();
		}

		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return "ip:" + forwarded.split(",")[0].trim();
		}
		return "ip:" + request.getRemoteAddr();
	}

	private Bucket newBucket() {
		Bandwidth limit = Bandwidth.builder()
				.capacity(rateLimitProperties.capacity())
				.refillIntervally(
						rateLimitProperties.refillTokens(),
						Duration.ofSeconds(rateLimitProperties.refillPeriodSeconds())
				)
				.build();
		return Bucket.builder().addLimit(limit).build();
	}

	private void writeRateLimitResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		jsonMapper.writeValue(
				response.getOutputStream(),
				ErrorResponse.of(
						HttpStatus.TOO_MANY_REQUESTS.value(),
						"RATE_LIMIT_EXCEEDED",
						"Too many order creation requests",
						request.getRequestURI()
				)
		);
	}
}
