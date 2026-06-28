package io.github.mohamedmedhat21.order_processing_system.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
		Instant timestamp,
		int status,
		String code,
		String message,
		String path,
		List<FieldError> errors
) {

	public record FieldError(String field, String message) {
	}

	public static ErrorResponse of(int status, String code, String message, String path) {
		return new ErrorResponse(Instant.now(), status, code, message, path, List.of());
	}

	public static ErrorResponse of(int status, String code, String message, String path, List<FieldError> errors) {
		return new ErrorResponse(Instant.now(), status, code, message, path, errors);
	}
}
