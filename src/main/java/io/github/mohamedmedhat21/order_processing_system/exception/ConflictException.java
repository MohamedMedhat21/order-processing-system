package io.github.mohamedmedhat21.order_processing_system.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BaseBusinessException {

	public ConflictException(String message) {
		super(HttpStatus.CONFLICT, "CONFLICT", message);
	}
}
