package io.github.mohamedmedhat21.order_processing_system.exception;

import java.util.List;

import io.github.mohamedmedhat21.order_processing_system.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BaseBusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(
			BaseBusinessException ex,
			HttpServletRequest request
	) {
		HttpStatus status = ex.getStatus();
		ErrorResponse body = ErrorResponse.of(
				status.value(),
				ex.getCode(),
				ex.getMessage(),
				request.getRequestURI()
		);
		return ResponseEntity.status(status).body(body);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(
			MethodArgumentNotValidException ex,
			HttpServletRequest request
	) {
		List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(this::toFieldError)
				.toList();

		ErrorResponse body = ErrorResponse.of(
				HttpStatus.BAD_REQUEST.value(),
				"VALIDATION_ERROR",
				"Request validation failed",
				request.getRequestURI(),
				fieldErrors
		);
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpectedException(
			Exception ex,
			HttpServletRequest request
	) {
		ErrorResponse body = ErrorResponse.of(
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				"INTERNAL_ERROR",
				"An unexpected error occurred",
				request.getRequestURI()
		);
		return ResponseEntity.internalServerError().body(body);
	}

	private ErrorResponse.FieldError toFieldError(FieldError fieldError) {
		return new ErrorResponse.FieldError(fieldError.getField(), fieldError.getDefaultMessage());
	}
}
