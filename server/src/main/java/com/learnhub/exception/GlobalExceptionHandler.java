package com.learnhub.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhub.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@Autowired
	private ObjectMapper objectMapper;

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
		logger.error("I/O error: {}", ex.getMessage());
		writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "I/O error: " + ex.getMessage());
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
