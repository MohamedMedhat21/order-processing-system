package io.github.mohamedmedhat21.order_processing_system.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BaseBusinessException {

	public ResourceNotFoundException(String resource, Long id) {
		super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "%s not found with id: %d".formatted(resource, id));
	}

	public ResourceNotFoundException(String message) {
		super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
	}
}
