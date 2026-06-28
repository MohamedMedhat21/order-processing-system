package io.github.mohamedmedhat21.order_processing_system.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends BaseBusinessException {

	public BadRequestException(String message) {
		super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
	}
}
