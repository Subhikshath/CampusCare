package com.CampusCare.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import com.CampusCare.ai.AIServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
	@SuppressWarnings("null")
	public class GlobalExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(AIServiceException.class)
	public ResponseEntity<ApiError> aiUnavailable(AIServiceException exception,
												 HttpServletRequest request) {
		logger.warn("AI request failed for {} {}: {}", request.getMethod(),
				request.getRequestURI(), exception.getMessage());
		return error(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
	}

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

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiError> notFound(ResourceNotFoundException exception,
															 HttpServletRequest request) {
		return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
	}

	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<ApiError> duplicate(DuplicateResourceException exception,
															 HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, exception.getMessage(), request);
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ApiError> badCredentials(BadCredentialsException exception,
																	   HttpServletRequest request) {
		return error(HttpStatus.UNAUTHORIZED, "Invalid email or password", request);
	}

	@ExceptionHandler(DisabledException.class)
	public ResponseEntity<ApiError> disabled(DisabledException exception,
															 HttpServletRequest request) {
		return error(HttpStatus.UNAUTHORIZED, "Invalid email or password", request);
	}

	@ExceptionHandler(UsernameNotFoundException.class)
	public ResponseEntity<ApiError> userNotFound(UsernameNotFoundException exception,
																	 HttpServletRequest request) {
		return error(HttpStatus.UNAUTHORIZED, "Invalid email or password", request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> accessDenied(AccessDeniedException exception,
																	 HttpServletRequest request) {
		return error(HttpStatus.FORBIDDEN, "You do not have permission to access this resource", request);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiError> dataIntegrity(DataIntegrityViolationException exception,
																	 HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, "The requested change conflicts with existing data", request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> internal(Exception exception,
											 HttpServletRequest request) {
		logger.error("Unhandled {} request for {}", request.getMethod(),
				request.getRequestURI(), exception);
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
