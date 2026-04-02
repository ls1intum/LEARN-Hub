package com.learnhub.controller;

import com.learnhub.dto.response.ErrorResponse;
import com.learnhub.dto.response.EnvironmentResponse;
import com.learnhub.dto.response.FieldValuesResponse;
import com.learnhub.dto.response.HelloResponse;
import com.learnhub.dto.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Meta", description = "Metadata and system information endpoints")
public class MetaController {

	private static final Logger logger = LoggerFactory.getLogger(MetaController.class);

	@Value("${app.environment:local}")
	private String environment;

	@GetMapping("/api/hello")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Health check", description = "Simple health check endpoint")
	public ResponseEntity<HelloResponse> hello() {
		logger.info("GET /api/hello - Health check endpoint called");
		return ResponseEntity.ok(new HelloResponse("Hello, world!"));
	}

	@GetMapping("/api/meta/field-values")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Get field values", description = "Get field values for enums used by client")
	public ResponseEntity<FieldValuesResponse> getFieldValues() {
		logger.info("GET /api/meta/field-values - Field values endpoint called");
		FieldValuesResponse fieldValues = new FieldValuesResponse(Arrays.asList("unplugged", "digital", "hybrid"),
				Arrays.asList("computers", "tablets", "handouts", "blocks", "electronics", "stationery"),
				Arrays.asList("remember", "understand", "apply", "analyze", "evaluate", "create"),
				Arrays.asList("decomposition", "patterns", "abstraction", "algorithms"),
				Arrays.asList("low", "medium", "high"), Arrays.asList("low", "medium", "high"),
				Arrays.asList("age_appropriateness", "bloom_level_match", "topic_relevance", "duration_fit"));
		return ResponseEntity.ok(fieldValues);
	}

	@GetMapping("/api/meta/environment")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Get current environment", description = "Get the current environment (local, staging, production)")
	public ResponseEntity<EnvironmentResponse> getEnvironment() {
		logger.info("GET /api/meta/environment - Environment endpoint called, environment={}", environment);
		return ResponseEntity.ok(new EnvironmentResponse(environment));
	}
}
