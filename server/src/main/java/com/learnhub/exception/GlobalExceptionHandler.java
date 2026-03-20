package com.learnhub.exception;

import com.learnhub.dto.response.ErrorResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Centralized exception handler for all REST controllers. Translates common
 * exception types to appropriate HTTP responses, eliminating repetitive
 * try-catch blocks in individual controllers.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
		logger.error("Resource not found: {}", ex.getMessage());
		return ResponseEntity.status(404).body(ErrorResponse.of(ex.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
		logger.error("Invalid argument: {}", ex.getMessage());
		return ResponseEntity.badRequest().body(ErrorResponse.of(ex.getMessage()));
	}

	@ExceptionHandler(IOException.class)
	public ResponseEntity<ErrorResponse> handleIOException(IOException ex) {
		logger.error("I/O error: {}", ex.getMessage());
		return ResponseEntity.status(500).body(ErrorResponse.of("I/O error: " + ex.getMessage()));
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
		logger.error("Unexpected error: {}", ex.getMessage());
		return ResponseEntity.status(500).body(ErrorResponse.of(ex.getMessage()));
	}
}
