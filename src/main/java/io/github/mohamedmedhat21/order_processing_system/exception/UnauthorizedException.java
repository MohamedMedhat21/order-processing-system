package io.github.mohamedmedhat21.order_processing_system.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BaseBusinessException {

	public UnauthorizedException(String message) {
		super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
	}
}
