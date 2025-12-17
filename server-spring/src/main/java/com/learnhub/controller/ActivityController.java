package com.learnhub.controller;

import com.learnhub.dto.response.ActivityResponse;
import com.learnhub.dto.response.ErrorResponse;
import com.learnhub.model.PDFDocument;
import com.learnhub.service.ActivityService;
import com.learnhub.service.PDFService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activities")
@Tag(name = "Activities", description = "Activity management and recommendations endpoints")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private PDFService pdfService;

    @GetMapping("/")
    @Operation(summary = "Get activities", description = "Get a list of activities with optional filtering and pagination")
    public ResponseEntity<?> getActivities(
            @RequestParam(required = false) String name,
            @RequestParam(name = "age_min", required = false) Integer ageMin,
            @RequestParam(name = "age_max", required = false) Integer ageMax,
            @RequestParam(required = false) List<String> format,
            @RequestParam(name = "bloom_level", required = false) List<String> bloomLevel,
            @RequestParam(required = false) List<String> resources_needed,
            @RequestParam(required = false) List<String> topics,
            @RequestParam(required = false, defaultValue = "100") Integer limit,
            @RequestParam(required = false, defaultValue = "0") Integer offset) {
        try {
            List<ActivityResponse> activities = activityService.getActivitiesWithFilters(
                    name, ageMin, ageMax, format, bloomLevel, limit, offset);
            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get activity by ID", description = "Get a single activity by its ID")
    public ResponseEntity<?> getActivity(@PathVariable Long id) {
        try {
            ActivityResponse activity = activityService.getActivityById(id);
            return ResponseEntity.ok(activity);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
        }
    }

    @PostMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create activity", description = "Create a new activity (admin only)")
    public ResponseEntity<?> createActivity(@RequestBody Map<String, Object> request) {
        try {
            // TODO: Implement create activity
            return ResponseEntity.ok(ResponseEntity.ok(new HashMap<>()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Delete activity", description = "Delete an activity by its ID (admin only)")
    public ResponseEntity<?> deleteActivity(@PathVariable Long id) {
        try {
            activityService.deleteActivity(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Activity deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
        }
    }

    @PostMapping("/upload-and-create")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Upload PDF and create activity", description = "Upload PDF, extract data, and create activity in one step (admin only)")
    public ResponseEntity<?> uploadAndCreateActivity(
            @RequestParam("pdf_file") MultipartFile pdfFile) {
        try {
            if (pdfFile.isEmpty()) {
                return ResponseEntity.badRequest().body(ErrorResponse.of("No PDF file provided"));
            }

            if (!pdfFile.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
                return ResponseEntity.badRequest().body(ErrorResponse.of("File must be a PDF"));
            }

            // TODO: Implement full upload and create logic
            byte[] pdfContent = pdfFile.getBytes();
            Long documentId = pdfService.storePdf(pdfContent, pdfFile.getOriginalFilename());

            Map<String, Object> response = new HashMap<>();
            response.put("document_id", documentId);
            response.put("message", "Activity created successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("Failed to upload and create activity: " + e.getMessage()));
        }
    }

    @GetMapping("/{activityId}/pdf")
    @Operation(summary = "Get activity PDF", description = "Get PDF file for a specific activity")
    public ResponseEntity<?> getActivityPdf(@PathVariable Long activityId) {
        try {
            ActivityResponse activity = activityService.getActivityById(activityId);
            if (activity.getDocumentId() == null) {
                return ResponseEntity.status(404).body(ErrorResponse.of("PDF not found for this activity"));
            }

            byte[] pdfContent = pdfService.getPdfContent(activity.getDocumentId());
            PDFDocument document = pdfService.getPdfDocument(activity.getDocumentId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", document.getFilename());
            headers.setContentLength(pdfContent.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfContent);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(ErrorResponse.of("PDF not found: " + e.getMessage()));
        }
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Get activity recommendations", description = "Get personalized activity recommendations")
    public ResponseEntity<?> getRecommendations(
            @RequestParam(required = false) Integer age_min,
            @RequestParam(required = false) Integer age_max,
            @RequestParam(required = false) List<String> format,
            @RequestParam(required = false) List<String> bloom_level,
            @RequestParam(required = false) List<String> resources_needed,
            @RequestParam(required = false) List<String> topics,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        try {
            // TODO: Implement recommendation logic
            List<ActivityResponse> activities = activityService.getActivitiesWithFilters(
                    null, age_min, age_max, format, bloom_level, limit, 0);
            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
        }
    }

    @PostMapping("/lesson-plan")
    @Operation(summary = "Generate lesson plan", description = "Generate a lesson plan from selected activities")
    public ResponseEntity<?> generateLessonPlan(@RequestBody Map<String, Object> request) {
        try {
            // TODO: Implement lesson plan generation
            Map<String, Object> response = new HashMap<>();
            response.put("lesson_plan", new HashMap<>());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
        }
    }
}
