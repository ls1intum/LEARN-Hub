package com.learnhub.controller;

import com.learnhub.dto.response.ErrorResponse;
import com.learnhub.dto.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "Meta", description = "Metadata and system information endpoints")
public class MetaController {

    @Value("${app.environment:local}")
    private String environment;

    @GetMapping("/api/hello")
    @Operation(summary = "Health check", description = "Simple health check endpoint")
    public ResponseEntity<Map<String, String>> hello() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Hello, world!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/hello")
    @Operation(summary = "Health check (alternative)", description = "Alternative health check endpoint")
    public ResponseEntity<Map<String, String>> helloAlt() {
        return hello();
    }

    @GetMapping("/api/meta/field-values")
    @Operation(summary = "Get field values", description = "Get field values for enums used by client")
    public ResponseEntity<?> getFieldValues() {
        Map<String, Object> fieldValues = new HashMap<>();
        fieldValues.put("format", Arrays.asList("unplugged", "digital", "hybrid"));
        fieldValues.put("resources_available", Arrays.asList("computers", "tablets", "handouts", "blocks", "electronics", "stationery"));
        fieldValues.put("bloom_level", Arrays.asList("remember", "understand", "apply", "analyze", "evaluate", "create"));
        fieldValues.put("topics", Arrays.asList("decomposition", "patterns", "abstraction", "algorithms"));
        fieldValues.put("mental_load", Arrays.asList("low", "medium", "high"));
        fieldValues.put("physical_energy", Arrays.asList("low", "medium", "high"));
        fieldValues.put("priority_categories", Arrays.asList("high_priority", "medium_priority", "low_priority"));
        return ResponseEntity.ok(fieldValues);
    }

    @GetMapping("/api/meta/environment")
    @Operation(summary = "Get current environment", description = "Get the current environment (local, staging, production)")
    public ResponseEntity<?> getEnvironment() {
        Map<String, String> response = new HashMap<>();
        response.put("environment", environment);
        return ResponseEntity.ok(response);
    }
}
