package io.github.mohamedmedhat21.order_processing_system.exception;

import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends BaseBusinessException {

	public RateLimitExceededException() {
		super(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "Too many order creation requests");
	}
}
