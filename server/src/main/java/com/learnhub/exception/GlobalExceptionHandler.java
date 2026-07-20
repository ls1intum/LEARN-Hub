package com.learnhub.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhub.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	// Spring Boot 4 defaults the web stack to Jackson 3, so the Jackson 2
	// ObjectMapper is no longer exposed as an injectable bean. This handler only
	// serialises a simple ErrorResponse DTO, so it owns a plain mapper - matching
	// the pattern used across the other services here.
	private final ObjectMapper objectMapper = new ObjectMapper();

	@ExceptionHandler(ResourceNotFoundException.class)
	public void handleResourceNotFound(ResourceNotFoundException ex, HttpServletResponse response) throws IOException {
		logger.error("Resource not found: {}", ex.getMessage());
		writeError(response, HttpServletResponse.SC_NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public void handleIllegalArgument(IllegalArgumentException ex, HttpServletResponse response) throws IOException {
		logger.error("Invalid argument: {}", ex.getMessage());
		writeError(response, HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(IOException.class)
	public void handleIOException(IOException ex, HttpServletResponse response) throws IOException {
		// Client disconnected mid-stream (common with SSE): the response is already
		// committed via getOutputStream(), so we can't - and shouldn't - write an error
		// body. Attempting to would throw IllegalStateException and flood the logs.
		if (isClientAbort(ex) || response.isCommitted()) {
			logger.debug("Client disconnected mid-response: {}", ex.getMessage());
			return;
		}
		logger.error("I/O error: {}", ex.getMessage());
		writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "I/O error: " + ex.getMessage());
	}

	private boolean isClientAbort(IOException ex) {
		String message = ex.getMessage();
		return message != null && (message.contains("Broken pipe") || message.contains("Connection reset"));
	}

	@ExceptionHandler(RuntimeException.class)
	public void handleRuntimeException(RuntimeException ex, HttpServletResponse response) throws IOException {
		logger.error("Unexpected error: {}", ex.getMessage(), ex);
		String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
		writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
	}

	private void writeError(HttpServletResponse response, int status, String message) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		objectMapper.writeValue(response.getWriter(), ErrorResponse.of(message));
	}
}
