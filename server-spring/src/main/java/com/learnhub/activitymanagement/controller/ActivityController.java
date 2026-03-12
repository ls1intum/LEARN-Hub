package com.learnhub.activitymanagement.controller;

import com.learnhub.activitymanagement.dto.request.ActivityFilterRequest;
import com.learnhub.activitymanagement.dto.request.LessonPlanInfoRequest;
import com.learnhub.activitymanagement.dto.request.LessonPlanRequest;
import com.learnhub.activitymanagement.dto.request.RecommendationRequest;
import com.learnhub.activitymanagement.dto.response.ActivityResponse;
import com.learnhub.activitymanagement.dto.response.LessonPlanInfoResponse;
import com.learnhub.activitymanagement.service.ActivityService;
import com.learnhub.activitymanagement.service.RecommendationService;
import com.learnhub.activitymanagement.service.ScoringEngineService;
import com.learnhub.documentmanagement.service.ArtikulationsschemaService;
import com.learnhub.documentmanagement.service.LLMService;
import com.learnhub.documentmanagement.service.PDFService;
import com.learnhub.dto.response.ErrorResponse;
import com.learnhub.usermanagement.service.UserSearchHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/activities")
@Tag(name = "Activities", description = "Activity management and recommendations endpoints")
public class ActivityController {

	private static final Logger logger = LoggerFactory.getLogger(ActivityController.class);

	@Autowired
	private ActivityService activityService;

	@Autowired
	private PDFService pdfService;

	@Autowired
	private RecommendationService recommendationService;

	@Autowired
	private UserSearchHistoryService searchHistoryService;

	@Autowired
	private LLMService llmService;

	@Autowired
	private ArtikulationsschemaService artikulationsschemaService;

	@GetMapping("/")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Get activities", description = "Get a list of activities with optional filtering and pagination")
	public ResponseEntity<?> getActivities(@ModelAttribute ActivityFilterRequest request) {
		logger.info(
				"GET /api/activities/ - Get activities called with filters: name={}, ageMin={}, ageMax={}, format={}, limit={}, offset={}",
				request.name(), request.ageMin(), request.ageMax(), request.format(), request.limit(),
				request.offset());
		try {
			List<ActivityResponse> activities = activityService.getActivitiesWithFilters(request.name(),
					request.ageMin(), request.ageMax(), request.durationMin(), request.durationMax(), request.format(),
					request.bloomLevel(), request.mentalLoad(), request.physicalEnergy(), request.resourcesNeeded(),
					request.topics(), request.limit(), request.offset());
			Map<String, Object> response = new HashMap<>();
			response.put("total",
					activityService.countActivitiesWithFilters(request.name(), request.ageMin(), request.ageMax(),
							request.durationMin(), request.durationMax(), request.format(), request.bloomLevel(),
							request.mentalLoad(), request.physicalEnergy(), request.resourcesNeeded(),
							request.topics()));
			response.put("activities", activities);
			response.put("limit", request.limit());
			response.put("offset", request.offset());
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			logger.error("GET /api/activities/ - Failed to retrieve activities: {}", e.getMessage());
			return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
		}
	}

	@GetMapping("/{id}")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Get activity by ID", description = "Get a single activity by its ID")
	public ResponseEntity<?> getActivity(@PathVariable UUID id) {
		logger.info("GET /api/activities/{} - Get activity by ID called", id);
		try {
			ActivityResponse activity = activityService.getActivityById(id);
			return ResponseEntity.ok(activity);
		} catch (Exception e) {
			logger.error("GET /api/activities/{} - Activity not found: {}", id, e.getMessage());
			return ResponseEntity.status(404).body(ErrorResponse.of(e.getMessage()));
		}
	}

	@PostMapping("/create")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Create activity", description = "Create a new activity (admin only)")
	public ResponseEntity<?> createActivity(@RequestBody Map<String, Object> request) {
		logger.info("POST /api/activities/create - Create activity called");
		try {
			ActivityResponse saved = activityService.createActivityWithValidation(request);
			logger.info("POST /api/activities/create - Activity created with id={}", saved.getId());
			Map<String, Object> response = new HashMap<>();
			response.put("activity", saved);
			return ResponseEntity.status(201).body(response);
		} catch (IllegalArgumentException e) {
			logger.error("POST /api/activities/create - Invalid activity data: {}", e.getMessage());
			return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
		} catch (Exception e) {
			logger.error("POST /api/activities/create - Failed to create activity: {}", e.getMessage());
			return ResponseEntity.status(500).body(ErrorResponse.of("Failed to create activity: " + e.getMessage()));
		}
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Delete activity", description = "Delete an activity by its ID (admin only)")
	public ResponseEntity<?> deleteActivity(@PathVariable UUID id) {
		logger.info("DELETE /api/activities/{} - Delete activity called", id);
		try {
			activityService.deleteActivity(id);
			logger.info("DELETE /api/activities/{} - Activity deleted successfully", id);
			Map<String, String> response = new HashMap<>();
			response.put("message", "Activity deleted successfully");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			logger.error("DELETE /api/activities/{} - Failed to delete activity: {}", id, e.getMessage());
			return ResponseEntity.status(404).body(ErrorResponse.of(e.getMessage()));
		}
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Update activity", description = "Update an existing activity's metadata and artikulationsschema (admin only)")
	public ResponseEntity<?> updateActivity(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
		logger.info("PUT /api/activities/{} - Update activity called", id);
		try {
			ActivityResponse updated = activityService.updateActivityFromMap(id, request);
			logger.info("PUT /api/activities/{} - Activity updated successfully", id);
			return ResponseEntity.ok(updated);
		} catch (IllegalArgumentException e) {
			logger.error("PUT /api/activities/{} - Invalid activity data: {}", id, e.getMessage());
			return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
		} catch (Exception e) {
			String msg = e.getMessage();
			if (msg != null && msg.contains("Activity not found")) {
				logger.error("PUT /api/activities/{} - Activity not found: {}", id, msg);
				return ResponseEntity.status(404).body(ErrorResponse.of(msg));
			}
			logger.error("PUT /api/activities/{} - Failed to update activity: {}", id, msg);
			return ResponseEntity.status(500).body(ErrorResponse.of("Failed to update activity: " + msg));
		}
	}

	@GetMapping("/{activityId}/pdf")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Get activity PDF", description = "Get PDF file for a specific activity")
	public ResponseEntity<?> getActivityPdf(@PathVariable UUID activityId) {
		logger.info("GET /api/activities/{}/pdf - Get activity PDF called", activityId);
		try {
			ActivityResponse activity = activityService.getActivityById(activityId);
			if (activity.getDocumentId() == null) {
				logger.error("GET /api/activities/{}/pdf - No PDF associated with this activity", activityId);
				return ResponseEntity.status(404).body(ErrorResponse.of("PDF not found for this activity"));
			}

			byte[] pdfContent = pdfService.getPdfContent(activity.getDocumentId());

			// Use activity name as download filename, sanitized for safety
			String activityName = activity.getName() != null ? activity.getName() : "activity";
			String downloadName = sanitizeDownloadFilename(activityName) + ".pdf";

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.setContentDispositionFormData("inline", downloadName);
			headers.setContentLength(pdfContent.length);

			return ResponseEntity.ok().headers(headers).body(pdfContent);
		} catch (Exception e) {
			logger.error("GET /api/activities/{}/pdf - PDF not found: {}", activityId, e.getMessage());
			return ResponseEntity.status(404).body(ErrorResponse.of("PDF not found: " + e.getMessage()));
		}
	}

	@GetMapping("/{activityId}/artikulationsschema-pdf")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Get Artikulationsschema PDF", description = "Get the Artikulationsschema PDF for a specific activity, rendered from saved markdown")
	public ResponseEntity<?> getArtikulationsschemaPdf(@PathVariable UUID activityId) {
		logger.info("GET /api/activities/{}/artikulationsschema-pdf - Get Artikulationsschema PDF called", activityId);
		try {
			ActivityResponse activity = activityService.getActivityById(activityId);
			String markdown = activity.getArtikulationsschemaMarkdown();
			if (markdown == null || markdown.trim().isEmpty()) {
				logger.error("GET /api/activities/{}/artikulationsschema-pdf - No Artikulationsschema for this activity",
						activityId);
				return ResponseEntity.status(404)
						.body(ErrorResponse.of("No Artikulationsschema found for this activity"));
			}

			byte[] pdfBytes = artikulationsschemaService.renderMarkdownToPdf(markdown);

			String activityName = activity.getName() != null ? activity.getName() : "activity";
			String downloadName = sanitizeDownloadFilename(activityName) + "_artikulationsschema.pdf";

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.setContentDispositionFormData("inline", downloadName);
			headers.setContentLength(pdfBytes.length);

			return ResponseEntity.ok().headers(headers).body(pdfBytes);
		} catch (Exception e) {
			logger.error("GET /api/activities/{}/artikulationsschema-pdf - Failed: {}", activityId, e.getMessage());
			return ResponseEntity.status(404)
					.body(ErrorResponse.of("Artikulationsschema PDF not found: " + e.getMessage()));
		}
	}

	@GetMapping("/recommendations")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Get activity recommendations", description = "Get personalized activity recommendations with scoring")
	public ResponseEntity<?> getRecommendations(@ModelAttribute RecommendationRequest request,
			HttpServletRequest httpRequest) {
		logger.info(
				"GET /api/activities/recommendations - Get recommendations called with targetAge={}, format={}, maxActivityCount={}, limit={}",
				request.targetAge(), request.format(), request.maxActivityCount(), request.limit());
		try {
			// Build criteria map using service
			Map<String, Object> criteria = activityService.buildRecommendationCriteria(request.name(),
					request.targetAge(), request.format(), request.bloomLevels(), request.targetDuration(),
					request.availableResources(), request.preferredTopics(), request.priorityCategories());

			// Save search history if user is authenticated
			UUID userId = (UUID) httpRequest.getAttribute("userId");
			if (userId != null) {
				searchHistoryService.saveSearchQuery(userId, criteria);
			}

			// Get recommendations from service
			Map<String, Object> response = recommendationService.getRecommendations(criteria, request.includeBreaks(),
					request.maxActivityCount(), request.limit());

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			logger.error("GET /api/activities/recommendations - Failed to get recommendations: {}", e.getMessage());
			return ResponseEntity.status(500)
					.body(ErrorResponse.of("Failed to get recommendations: " + e.getMessage()));
		}
	}

	@GetMapping("/scoring-insights")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Get scoring insights", description = "Get information about scoring categories and their weights")
	public ResponseEntity<?> getScoringInsights() {
		logger.info("GET /api/activities/scoring-insights - Get scoring insights called");
		try {
			Map<String, ScoringEngineService.ScoringCategory> categories = ScoringEngineService.getScoringCategories();

			Map<String, Object> response = new HashMap<>();
			Map<String, Map<String, Object>> categoriesMap = new HashMap<>();

			for (Map.Entry<String, ScoringEngineService.ScoringCategory> entry : categories.entrySet()) {
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
			logger.error("GET /api/activities/scoring-insights - Failed to get scoring insights: {}", e.getMessage());
			return ResponseEntity.status(500)
					.body(ErrorResponse.of("Failed to get scoring insights: " + e.getMessage()));
		}
	}

	@PostMapping("/lesson-plan")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Generate lesson plan", description = "Generate a lesson plan from selected activities")
	public ResponseEntity<?> generateLessonPlan(@RequestBody LessonPlanRequest request) {
		logger.info("POST /api/activities/lesson-plan - Generate lesson plan called with {} activities",
				request.getActivities() != null ? request.getActivities().size() : 0);
		try {
			List<Map<String, Object>> activities = request.getActivities();

			// Check if PDFs are available
			LessonPlanInfoResponse info = pdfService.getLessonPlanInfo(activities);
			if (!info.isCanGenerateLessonPlan()) {
				logger.error("POST /api/activities/lesson-plan - No PDFs available for the selected activities");
				return ResponseEntity.badRequest()
						.body(ErrorResponse.of("No PDFs available for the selected activities"));
			}

			// Process breaks using service
			List<Map<String, Object>> breaks = activityService.processLessonPlanBreaks(activities, request.getBreaks());

			// Generate lesson plan PDF
			byte[] lessonPlanPdf = pdfService.generateLessonPlan(activities, request.getSearchCriteria(), breaks,
					request.getTotalDuration());

			logger.info("POST /api/activities/lesson-plan - Lesson plan PDF generated successfully, size={} bytes",
					lessonPlanPdf.length);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.setContentDispositionFormData("attachment", "lesson_plan.pdf");
			headers.setContentLength(lessonPlanPdf.length);

			return ResponseEntity.ok().headers(headers).body(lessonPlanPdf);
		} catch (Exception e) {
			logger.error("POST /api/activities/lesson-plan - Failed to generate lesson plan: {}", e.getMessage());
			return ResponseEntity.status(500)
					.body(ErrorResponse.of("Failed to generate lesson plan: " + e.getMessage()));
		}
	}

	@PostMapping("/lesson-plan/info")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Get lesson plan info", description = "Get lesson plan generation information")
	public ResponseEntity<?> getLessonPlanInfo(@RequestBody LessonPlanInfoRequest request) {
		logger.info("POST /api/activities/lesson-plan/info - Get lesson plan info called with {} activities",
				request.getActivities() != null ? request.getActivities().size() : 0);
		try {
			LessonPlanInfoResponse info = pdfService.getLessonPlanInfo(request.getActivities());
			return ResponseEntity.ok(info);
		} catch (Exception e) {
			logger.error("POST /api/activities/lesson-plan/info - Failed to get lesson plan info: {}", e.getMessage());
			return ResponseEntity.status(500)
					.body(ErrorResponse.of("Failed to get lesson plan info: " + e.getMessage()));
		}
	}

	@PostMapping("/generate-artikulationsschema")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Generate Artikulationsschema", description = "Generate or extract an Artikulationsschema markdown from an uploaded PDF (admin only)")
	public ResponseEntity<?> generateArtikulationsschema(@RequestBody Map<String, Object> request) {
		logger.info("POST /api/activities/generate-artikulationsschema called");
		try {
			Object documentIdObj = request.get("document_id");
			if (documentIdObj == null) {
				return ResponseEntity.badRequest().body(ErrorResponse.of("document_id is required"));
			}

			UUID documentId;
			try {
				documentId = UUID.fromString(documentIdObj.toString());
			} catch (IllegalArgumentException e) {
				return ResponseEntity.badRequest().body(ErrorResponse.of("Invalid document_id format"));
			}

			// Get PDF text from cached or persisted PDF
			String pdfText = pdfService.extractTextFromPdf(documentId);
			if (pdfText == null || pdfText.trim().length() < 10) {
				return ResponseEntity.badRequest()
						.body(ErrorResponse.of("PDF does not contain sufficient text for schema generation"));
			}

			String markdown = llmService.generateArtikulationsschema(pdfText);

			Map<String, Object> response = new HashMap<>();
			response.put("markdown", markdown);
			response.put("document_id", documentId.toString());

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			logger.error("POST /api/activities/generate-artikulationsschema - Failed: {}", e.getMessage());
			return ResponseEntity.status(500)
					.body(ErrorResponse.of("Failed to generate Artikulationsschema: " + e.getMessage()));
		}
	}

	@PostMapping("/preview-artikulationsschema-pdf")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Preview Artikulationsschema PDF", description = "Render Artikulationsschema markdown to a preview PDF (admin only)")
	public ResponseEntity<?> previewArtikulationsschemaPdf(@RequestBody Map<String, String> request) {
		logger.info("POST /api/activities/preview-artikulationsschema-pdf called");
		try {
			String markdown = request.get("markdown");
			if (markdown == null || markdown.trim().isEmpty()) {
				return ResponseEntity.badRequest().body(ErrorResponse.of("markdown is required"));
			}

			byte[] pdfBytes = artikulationsschemaService.renderMarkdownToPdf(markdown);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.setContentDispositionFormData("inline", "artikulationsschema_preview.pdf");
			headers.setContentLength(pdfBytes.length);

			return ResponseEntity.ok().headers(headers).body(pdfBytes);
		} catch (Exception e) {
			logger.error("POST /api/activities/preview-artikulationsschema-pdf - Failed: {}", e.getMessage());
			return ResponseEntity.status(500).body(ErrorResponse.of("Failed to render preview PDF: " + e.getMessage()));
		}
	}

	@PostMapping("/upload-pdf-draft")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Upload PDF for draft activity", description = "Upload a PDF and cache it, returning a document_id and extracted metadata for the 2-step creation flow (admin only)")
	public ResponseEntity<?> uploadPdfDraft(@RequestParam("pdf_file") MultipartFile pdfFile) {
		logger.info("POST /api/activities/upload-pdf-draft - Upload PDF draft called with file={}",
				pdfFile.getOriginalFilename());
		try {
			Map<String, Object> result = activityService.uploadPdfAndExtractMetadata(pdfFile);
			return ResponseEntity.status(201).body(result);
		} catch (IllegalArgumentException e) {
			logger.error("POST /api/activities/upload-pdf-draft - Invalid input: {}", e.getMessage());
			return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
		} catch (Exception e) {
			logger.error("POST /api/activities/upload-pdf-draft - Failed: {}", e.getMessage());
			return ResponseEntity.status(500).body(ErrorResponse.of("Failed to upload PDF draft: " + e.getMessage()));
		}
	}

	private String sanitizeDownloadFilename(String name) {
		if (name == null || name.isBlank()) {
			return "activity";
		}
		String sanitized = name.replaceAll("[^a-zA-Z0-9._\\- ]", "_").trim();
		return sanitized.isEmpty() ? "activity" : sanitized;
	}
}
