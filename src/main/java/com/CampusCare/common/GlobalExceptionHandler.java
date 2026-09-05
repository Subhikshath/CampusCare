package com.CampusCare.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception,
											   HttpServletRequest request) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining(", "));
		return error(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> badRequest(IllegalArgumentException exception,
											   HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> internal(Exception exception,
											 HttpServletRequest request) {
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request);
	}

	private ResponseEntity<ApiError> error(HttpStatus status, String message,
										   HttpServletRequest request) {
		return ResponseEntity.status(status).body(new ApiError(
				LocalDateTime.now(), status.value(), status.getReasonPhrase(),
				message, request.getRequestURI()));
	}

	public record ApiError(LocalDateTime timestamp, int status, String error,
						   String message, String path) {
	}
}
