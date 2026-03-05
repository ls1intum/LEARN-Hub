package com.learnhub.controller;

import com.learnhub.dto.response.ErrorResponse;
import com.learnhub.dto.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "Meta", description = "Metadata and system information endpoints")
public class MetaController {

    private static final Logger logger = LoggerFactory.getLogger(MetaController.class);

    @Value("${app.environment:local}")
    private String environment;

    @GetMapping("/api/hello")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Health check", description = "Simple health check endpoint")
    public ResponseEntity<Map<String, String>> hello() {
        logger.info("GET /api/hello - Health check endpoint called");
        Map<String, String> response = new HashMap<>();
        response.put("message", "Hello, world!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/meta/field-values")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get field values", description = "Get field values for enums used by client")
    public ResponseEntity<?> getFieldValues() {
        logger.info("GET /api/meta/field-values - Field values endpoint called");
        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("format", Arrays.asList("unplugged", "digital", "hybrid"));
        fieldValues.put("resources_available", Arrays.asList("computers", "tablets", "handouts", "blocks", "electronics", "stationery"));
        fieldValues.put("bloom_level", Arrays.asList("remember", "understand", "apply", "analyze", "evaluate", "create"));
        fieldValues.put("topics", Arrays.asList("decomposition", "patterns", "abstraction", "algorithms"));
        fieldValues.put("mental_load", Arrays.asList("low", "medium", "high"));
        fieldValues.put("physical_energy", Arrays.asList("low", "medium", "high"));
        fieldValues.put("priority_categories", Arrays.asList("age_appropriateness", "bloom_level_match", "topic_relevance", "duration_fit"));
        return ResponseEntity.ok(fieldValues);
    }

    @GetMapping("/api/meta/environment")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get current environment", description = "Get the current environment (local, staging, production)")
    public ResponseEntity<?> getEnvironment() {
        logger.info("GET /api/meta/environment - Environment endpoint called, environment={}", environment);
        Map<String, String> response = new HashMap<>();
        response.put("environment", environment);
        return ResponseEntity.ok(response);
    }
}
