package io.github.mohamedmedhat21.order_processing_system.exception;

import org.springframework.http.HttpStatus;

public abstract class BaseBusinessException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	protected BaseBusinessException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}
}
