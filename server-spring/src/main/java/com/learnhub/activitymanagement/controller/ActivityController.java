package com.learnhub.activitymanagement.controller;

import com.learnhub.activitymanagement.dto.request.LessonPlanInfoRequest;
import com.learnhub.activitymanagement.dto.request.LessonPlanRequest;
import com.learnhub.activitymanagement.dto.response.ActivityResponse;
import com.learnhub.dto.response.ErrorResponse;
import com.learnhub.activitymanagement.dto.response.LessonPlanInfoResponse;
import com.learnhub.documentmanagement.entity.PDFDocument;
import com.learnhub.activitymanagement.service.ActivityService;
import com.learnhub.documentmanagement.service.PDFService;
import com.learnhub.activitymanagement.service.RecommendationService;
import com.learnhub.activitymanagement.service.ScoringEngine;
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

    @Autowired
    private RecommendationService recommendationService;

    @GetMapping("/")
    @Operation(summary = "Get activities", description = "Get a list of activities with optional filtering and pagination")
    public ResponseEntity<?> getActivities(
            @RequestParam(required = false) String name,
            @RequestParam(name = "age_min", required = false) Integer ageMin,
            @RequestParam(name = "age_max", required = false) Integer ageMax,
            @RequestParam(required = false) List<String> format,
            @RequestParam(name = "bloom_level", required = false) List<String> bloomLevel,
            @RequestParam(name = "mental_load", required = false) String mentalLoad,
            @RequestParam(name = "physical_energy", required = false) String physicalEnergy,
            @RequestParam(required = false) List<String> resources_needed,
            @RequestParam(required = false) List<String> topics,
            @RequestParam(required = false, defaultValue = "100") Integer limit,
            @RequestParam(required = false, defaultValue = "0") Integer offset) {
        try {
            List<ActivityResponse> activities = activityService.getActivitiesWithFilters(
                    name, ageMin, ageMax, format, bloomLevel, mentalLoad, physicalEnergy, limit, offset);
            Map<String, Object> response = new HashMap<>();
            response.put("total", activities.size());
            response.put("activities", activities);
            return ResponseEntity.ok(response);
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
            ActivityResponse saved = activityService.createActivityWithValidation(request);
            Map<String, Object> response = new HashMap<>();
            response.put("activity", saved);
            return ResponseEntity.status(201).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ErrorResponse.of("Failed to create activity: " + e.getMessage()));
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
            Map<String, Object> response = activityService.uploadAndCreateActivity(pdfFile);
            return ResponseEntity.status(201).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
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
    @Operation(summary = "Get activity recommendations", description = "Get personalized activity recommendations with scoring")
    public ResponseEntity<?> getRecommendations(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer target_age,
            @RequestParam(required = false) List<String> format,
            @RequestParam(required = false) List<String> bloom_levels,
            @RequestParam(required = false) Integer target_duration,
            @RequestParam(required = false) List<String> available_resources,
            @RequestParam(required = false) List<String> preferred_topics,
            @RequestParam(required = false) List<String> priority_categories,
            @RequestParam(required = false, defaultValue = "false") Boolean include_breaks,
            @RequestParam(required = false, defaultValue = "2") Integer max_activity_count,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        try {
            // Build criteria map using service
            Map<String, Object> criteria = activityService.buildRecommendationCriteria(
                    name, target_age, format, bloom_levels, target_duration,
                    available_resources, preferred_topics, priority_categories);

            // Get recommendations from service
            Map<String, Object> response = recommendationService.getRecommendations(
                    criteria, include_breaks, max_activity_count, limit);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("Failed to get recommendations: " + e.getMessage()));
        }
    }

    @GetMapping("/scoring-insights")
    @Operation(summary = "Get scoring insights", description = "Get information about scoring categories and their weights")
    public ResponseEntity<?> getScoringInsights() {
        try {
            Map<String, ScoringEngine.ScoringCategory> categories = ScoringEngine.getScoringCategories();

            Map<String, Object> response = new HashMap<>();
            Map<String, Map<String, Object>> categoriesMap = new HashMap<>();

            for (Map.Entry<String, ScoringEngine.ScoringCategory> entry : categories.entrySet()) {
                Map<String, Object> categoryInfo = new HashMap<>();
                categoryInfo.put("name", entry.getValue().getName());
                categoryInfo.put("impact", entry.getValue().getImpact());
                categoryInfo.put("description", entry.getValue().getDescription());
                categoriesMap.put(entry.getKey(), categoryInfo);
            }

            response.put("categories", categoriesMap);
            response.put("description", "Scoring categories used to evaluate activity recommendations");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("Failed to get scoring insights: " + e.getMessage()));
        }
    }

    @PostMapping("/lesson-plan")
    @Operation(summary = "Generate lesson plan", description = "Generate a lesson plan from selected activities")
    public ResponseEntity<?> generateLessonPlan(@RequestBody LessonPlanRequest request) {
        try {
            List<Map<String, Object>> activities = request.getActivities();

            // Check if PDFs are available
            LessonPlanInfoResponse info = pdfService.getLessonPlanInfo(activities);
            if (!info.isCanGenerateLessonPlan()) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("No PDFs available for the selected activities"));
            }

            // Process breaks using service
            List<Map<String, Object>> breaks = activityService.processLessonPlanBreaks(
                    activities, request.getBreaks());

            // Generate lesson plan PDF
            byte[] lessonPlanPdf = pdfService.generateLessonPlan(
                    activities,
                    request.getSearchCriteria(),
                    breaks,
                    request.getTotalDuration());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "lesson_plan.pdf");
            headers.setContentLength(lessonPlanPdf.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(lessonPlanPdf);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("Failed to generate lesson plan: " + e.getMessage()));
        }
    }

    @PostMapping("/lesson-plan/info")
    @Operation(summary = "Get lesson plan info", description = "Get lesson plan generation information")
    public ResponseEntity<?> getLessonPlanInfo(@RequestBody LessonPlanInfoRequest request) {
        try {
            LessonPlanInfoResponse info = pdfService.getLessonPlanInfo(request.getActivities());
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("Failed to get lesson plan info: " + e.getMessage()));
        }
    }
}
