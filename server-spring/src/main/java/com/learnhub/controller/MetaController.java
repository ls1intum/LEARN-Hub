package com.learnhub.controller;

import com.learnhub.dto.response.ErrorResponse;
import com.learnhub.dto.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "Meta", description = "Metadata and system information endpoints")
public class MetaController {

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

    @GetMapping("/api/meta/formats")
    @Operation(summary = "Get activity formats", description = "Get list of available activity formats")
    public ResponseEntity<?> getFormats() {
        return ResponseEntity.ok(new String[]{"unplugged", "digital", "hybrid"});
    }

    @GetMapping("/api/meta/bloom-levels")
    @Operation(summary = "Get Bloom levels", description = "Get list of available Bloom taxonomy levels")
    public ResponseEntity<?> getBloomLevels() {
        return ResponseEntity.ok(
            new String[]{"remember", "understand", "apply", "analyze", "evaluate", "create"}
        );
    }

    @GetMapping("/api/meta/resources")
    @Operation(summary = "Get resources", description = "Get list of available resources")
    public ResponseEntity<?> getResources() {
        return ResponseEntity.ok(
            new String[]{"computers", "tablets", "handouts", "blocks", "electronics", "stationery"}
        );
    }

    @GetMapping("/api/meta/topics")
    @Operation(summary = "Get topics", description = "Get list of available computational thinking topics")
    public ResponseEntity<?> getTopics() {
        return ResponseEntity.ok(
            new String[]{"decomposition", "patterns", "abstraction", "algorithms"}
        );
    }

    @GetMapping("/api/meta/energy-levels")
    @Operation(summary = "Get energy levels", description = "Get list of available energy levels")
    public ResponseEntity<?> getEnergyLevels() {
        return ResponseEntity.ok(new String[]{"low", "medium", "high"});
    }
}
