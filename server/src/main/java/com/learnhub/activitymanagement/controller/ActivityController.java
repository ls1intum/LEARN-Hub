package com.learnhub.activitymanagement.controller;

import com.learnhub.activitymanagement.dto.request.ActivityFilterRequest;
import com.learnhub.activitymanagement.dto.request.LessonPlanInfoRequest;
import com.learnhub.activitymanagement.dto.request.LessonPlanRequest;
import com.learnhub.activitymanagement.dto.request.RecommendationRequest;
import com.learnhub.activitymanagement.dto.response.ActivityResponse;
import com.learnhub.activitymanagement.dto.response.DocumentResponse;
import com.learnhub.activitymanagement.dto.response.LessonPlanInfoResponse;
import com.learnhub.activitymanagement.dto.response.MarkdownResponse;
import com.learnhub.activitymanagement.service.ActivityService;
import com.learnhub.activitymanagement.service.RecommendationService;
import com.learnhub.documentmanagement.service.LLMService;
import com.learnhub.documentmanagement.service.MarkdownToDocxService;
import com.learnhub.documentmanagement.service.MarkdownToPdfService;
import com.learnhub.documentmanagement.service.PDFService;
import com.learnhub.exception.ResourceNotFoundException;
import com.learnhub.usermanagement.service.UserSearchHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
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
	private static final String[] MARKDOWN_TYPE_ORDER = {"deckblatt", "artikulationsschema", "hintergrundwissen"};

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
	private MarkdownToPdfService markdownToPdfService;

	@Autowired
	private MarkdownToDocxService markdownToDocxService;

	@GetMapping("/")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Get activities", description = "Get a list of activities with optional filtering and pagination")
	public ResponseEntity<?> getActivities(@ModelAttribute ActivityFilterRequest request) {
		logger.info(
				"GET /api/activities/ - Get activities called with filters: name={}, ageMin={}, ageMax={}, format={}, limit={}, offset={}",
				request.name(), request.ageMin(), request.ageMax(), request.format(), request.limit(),
				request.offset());
		List<ActivityResponse> activities = activityService.getActivitiesWithFilters(request.name(), request.ageMin(),
				request.ageMax(), request.durationMin(), request.durationMax(), request.format(), request.bloomLevel(),
				request.mentalLoad(), request.physicalEnergy(), request.resourcesNeeded(), request.topics(),
				request.limit(), request.offset());
		Map<String, Object> response = new HashMap<>();
		response.put("total",
				activityService.countActivitiesWithFilters(request.name(), request.ageMin(), request.ageMax(),
						request.durationMin(), request.durationMax(), request.format(), request.bloomLevel(),
						request.mentalLoad(), request.physicalEnergy(), request.resourcesNeeded(), request.topics()));
		response.put("activities", activities);
		response.put("limit", request.limit());
		response.put("offset", request.offset());
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Get activity by ID", description = "Get a single activity by its ID")
	public ResponseEntity<?> getActivity(@PathVariable UUID id) {
		logger.info("GET /api/activities/{} - Get activity by ID called", id);
		ActivityResponse activity = activityService.getActivityById(id);
		return ResponseEntity.ok(activity);
	}

	@PostMapping("/create")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Create activity", description = "Create a new activity (admin only)")
	public ResponseEntity<?> createActivity(@RequestBody Map<String, Object> request) {
		logger.info("POST /api/activities/create - Create activity called");
		ActivityResponse saved = activityService.createActivityWithValidation(request);
		logger.info("POST /api/activities/create - Activity created with id={}", saved.getId());
		Map<String, Object> response = new HashMap<>();
		response.put("activity", saved);
		return ResponseEntity.status(201).body(response);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Delete activity", description = "Delete an activity by its ID (admin only)")
	public ResponseEntity<?> deleteActivity(@PathVariable UUID id) {
		logger.info("DELETE /api/activities/{} - Delete activity called", id);
		activityService.deleteActivity(id);
		logger.info("DELETE /api/activities/{} - Activity deleted successfully", id);
		Map<String, String> response = new HashMap<>();
		response.put("message", "Activity deleted successfully");
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Update activity", description = "Update an existing activity's metadata and artikulationsschema (admin only)")
	public ResponseEntity<?> updateActivity(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
		logger.info("PUT /api/activities/{} - Update activity called", id);
		ActivityResponse updated = activityService.updateActivityFromMap(id, request);
		logger.info("PUT /api/activities/{} - Activity updated successfully", id);
		return ResponseEntity.ok(updated);
	}

	@GetMapping("/{activityId}/pdf")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Get activity PDF", description = "Get PDF file for a specific activity")
	public ResponseEntity<?> getActivityPdf(@PathVariable UUID activityId) throws IOException {
		logger.info("GET /api/activities/{}/pdf - Get activity PDF called", activityId);
		ActivityResponse activity = activityService.getActivityById(activityId);
		DocumentResponse sourcePdf = activity.getDocuments().stream().filter(d -> "source_pdf".equals(d.getType()))
				.findFirst().orElse(null);
		if (sourcePdf == null) {
			throw new ResourceNotFoundException("PDF not found for this activity");
		}

		byte[] pdfContent = pdfService.getPdfContent(sourcePdf.getId());
		return buildFileDownloadResponse(pdfContent, activity.getName(), ".pdf", MediaType.APPLICATION_PDF, "inline");
	}

	@GetMapping("/recommendations")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Get activity recommendations", description = "Get personalized activity recommendations with scoring")
	public ResponseEntity<?> getRecommendations(@ModelAttribute RecommendationRequest request,
			HttpServletRequest httpRequest) {
		logger.info(
				"GET /api/activities/recommendations - Get recommendations called with targetAge={}, format={}, maxActivityCount={}, limit={}",
				request.targetAge(), request.format(), request.maxActivityCount(), request.limit());
		// Build criteria map using service
		Map<String, Object> criteria = activityService.buildRecommendationCriteria(request.name(), request.targetAge(),
				request.format(), request.bloomLevels(), request.targetDuration(), request.availableResources(),
				request.preferredTopics(), request.priorityCategories());

		// Save search history if user is authenticated
		UUID userId = (UUID) httpRequest.getAttribute("userId");
		if (userId != null) {
			searchHistoryService.saveSearchQuery(userId, criteria);
		}

		// Get recommendations from service
		Map<String, Object> response = recommendationService.getRecommendations(criteria, request.includeBreaks(),
				request.maxActivityCount(), request.limit());

		return ResponseEntity.ok(response);
	}

	@PostMapping("/lesson-plan")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Generate lesson plan", description = "Generate a lesson plan from selected activities")
	public ResponseEntity<?> generateLessonPlan(@RequestBody LessonPlanRequest request) throws IOException {
		logger.info("POST /api/activities/lesson-plan - Generate lesson plan called with {} activities",
				request.getActivities() != null ? request.getActivities().size() : 0);
		List<Map<String, Object>> activities = request.getActivities();

		// Check if PDFs are available
		LessonPlanInfoResponse info = pdfService.getLessonPlanInfo(activities);
		if (!info.isCanGenerateLessonPlan()) {
			throw new IllegalArgumentException("No PDFs available for the selected activities");
		}

		// Process breaks using service
		List<Map<String, Object>> breaks = activityService.processLessonPlanBreaks(activities, request.getBreaks());

		// Generate lesson plan PDF
		byte[] lessonPlanPdf = pdfService.generateLessonPlan(activities, request.getSearchCriteria(), breaks,
				request.getTotalDuration());

		logger.info("POST /api/activities/lesson-plan - Lesson plan PDF generated successfully, size={} bytes",
				lessonPlanPdf.length);

		return buildFileDownloadResponse(lessonPlanPdf, "lesson_plan", ".pdf", MediaType.APPLICATION_PDF, "inline");
	}

	@PostMapping("/lesson-plan/info")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Get lesson plan info", description = "Get lesson plan generation information")
	public ResponseEntity<?> getLessonPlanInfo(@RequestBody LessonPlanInfoRequest request) {
		logger.info("POST /api/activities/lesson-plan/info - Get lesson plan info called with {} activities",
				request.getActivities() != null ? request.getActivities().size() : 0);
		LessonPlanInfoResponse info = pdfService.getLessonPlanInfo(request.getActivities());
		return ResponseEntity.ok(info);
	}

	@PostMapping("/generate-artikulationsschema")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Generate Artikulationsschema", description = "Generate or extract an Artikulationsschema markdown from an uploaded PDF (admin only)")
	public ResponseEntity<?> generateArtikulationsschema(@RequestBody Map<String, Object> request) {
		logger.info("POST /api/activities/generate-artikulationsschema called");
		UUID documentId = parseRequiredDocumentId(request);

		// Get PDF text from cached or persisted PDF
		String pdfText = pdfService.extractTextFromPdf(documentId);
		if (pdfText == null || pdfText.trim().length() < 10) {
			throw new IllegalArgumentException("PDF does not contain sufficient text for schema generation");
		}

		// Extract user-adjusted metadata if provided
		@SuppressWarnings("unchecked")
		Map<String, Object> metadata = request.get("metadata") instanceof Map
				? (Map<String, Object>) request.get("metadata")
				: null;

		String markdown = llmService.generateArtikulationsschema(pdfText, metadata);

		Map<String, Object> response = new HashMap<>();
		response.put("markdown", markdown);
		response.put("documentId", documentId.toString());

		return ResponseEntity.ok(response);
	}

	@PostMapping("/upload-pdf-draft")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Upload PDF for draft activity", description = "Upload a PDF and cache it, returning a document_id and extracted metadata for the 2-step creation flow (admin only)")
	public ResponseEntity<?> uploadPdfDraft(@RequestParam("pdf_file") MultipartFile pdfFile,
			@RequestParam(value = "extractMetadata", defaultValue = "true") boolean extractMetadata) {
		logger.info("POST /api/activities/upload-pdf-draft - Upload PDF draft called with file={}",
				pdfFile.getOriginalFilename());
		Map<String, Object> result = activityService.uploadPdfAndExtractMetadata(pdfFile, extractMetadata);
		return ResponseEntity.status(201).body(result);
	}

	@PostMapping("/regenerate-metadata")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Regenerate metadata", description = "Re-run metadata extraction for a cached or persisted PDF (admin only)")
	public ResponseEntity<?> regenerateMetadata(@RequestBody Map<String, Object> request) {
		logger.info("POST /api/activities/regenerate-metadata called");
		UUID documentId = parseRequiredDocumentId(request);
		Map<String, Object> result = activityService.extractMetadataFromDocument(documentId);
		return ResponseEntity.ok(result);
	}

	@PostMapping("/generate-activity-markdowns")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Generate all activity markdowns", description = "Generate Deckblatt, Artikulationsschema, and Hintergrundwissen markdowns from an uploaded PDF (admin only)")
	public ResponseEntity<?> generateActivityMarkdowns(@RequestBody Map<String, Object> request) {
		logger.info("POST /api/activities/generate-activity-markdowns called");
		UUID documentId = parseRequiredDocumentId(request);

		String pdfText = pdfService.extractTextFromPdf(documentId);
		if (pdfText == null || pdfText.trim().length() < 10) {
			throw new IllegalArgumentException("PDF does not contain sufficient text for generation");
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> metadata = request.get("metadata") instanceof Map
				? (Map<String, Object>) request.get("metadata")
				: null;

		// Determine which types to generate
		@SuppressWarnings("unchecked")
		List<String> types = request.get("types") instanceof List ? (List<String>) request.get("types") : null;

		Map<String, Object> response = new HashMap<>();
		response.put("documentId", documentId.toString());

		boolean generateAll = types == null || types.isEmpty();

		if (generateAll || types.contains("deckblatt")) {
			String deckblatt = llmService.generateDeckblatt(pdfText, metadata);
			response.put("deckblattMarkdown", deckblatt);
		}

		if (generateAll || types.contains("artikulationsschema")) {
			String artikulationsschema = llmService.generateArtikulationsschema(pdfText, metadata);
			response.put("artikulationsschemaMarkdown", artikulationsschema);
		}

		if (generateAll || types.contains("hintergrundwissen")) {
			String hintergrundwissen = llmService.generateHintergrundwissen(pdfText, metadata);
			response.put("hintergrundwissenMarkdown", hintergrundwissen);
		}

		return ResponseEntity.ok(response);
	}

	@GetMapping("/{activityId}/download-pdf")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Download activity as combined PDF", description = "Download all markdown files (Deckblatt portrait, Artikulationsschema landscape, Hintergrundwissen portrait) as a single PDF")
	public ResponseEntity<?> downloadActivityPdf(@PathVariable UUID activityId) {
		logger.info("GET /api/activities/{}/download-pdf - Download combined activity PDF", activityId);
		ActivityResponse activity = activityService.getActivityById(activityId);
		List<byte[]> pdfParts = buildOrderedPdfParts(activity);

		if (pdfParts.isEmpty()) {
			throw new ResourceNotFoundException("No markdown content available for this activity");
		}

		byte[] pdfBytes = pdfParts.size() == 1 ? pdfParts.get(0) : markdownToPdfService.mergePdfs(pdfParts);

		return buildFileDownloadResponse(pdfBytes, activity.getName(), ".pdf", MediaType.APPLICATION_PDF, "inline");
	}

	@GetMapping("/{activityId}/download-docx")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Download activity as combined DOCX", description = "Download all markdown files (Deckblatt portrait, Artikulationsschema landscape, Hintergrundwissen portrait) as a single DOCX")
	public ResponseEntity<?> downloadActivityDocx(@PathVariable UUID activityId) {
		logger.info("GET /api/activities/{}/download-docx - Download combined activity DOCX", activityId);
		ActivityResponse activity = activityService.getActivityById(activityId);
		List<String> markdowns = new ArrayList<>();
		List<Boolean> landscapes = new ArrayList<>();
		buildOrderedDocxParts(activity, markdowns, landscapes);

		if (markdowns.isEmpty()) {
			throw new ResourceNotFoundException("No markdown content available for this activity");
		}

		String activityName = activity.getName() != null ? activity.getName() : "";
		byte[] docxBytes = markdownToDocxService.renderMergedDocx(markdowns, landscapes, activityName);

		MediaType docxMediaType = MediaType
				.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		return buildFileDownloadResponse(docxBytes, activityName.isEmpty() ? "activity" : activityName, ".docx",
				docxMediaType, "attachment");
	}

	/**
	 * Build ordered list of PDF parts, each rendered with the orientation stored in
	 * the DB. Order: Deckblatt, Artikulationsschema, Hintergrundwissen.
	 */
	private List<byte[]> buildOrderedPdfParts(ActivityResponse activity) {

		String activityName = activity.getName() != null ? activity.getName() : "";

		List<byte[]> parts = new ArrayList<>();
		if (activity.getMarkdowns() == null) {
			return parts;
		}

		for (String type : MARKDOWN_TYPE_ORDER) {
			for (MarkdownResponse md : activity.getMarkdowns()) {
				if (type.equals(md.getType()) && md.getContent() != null && !md.getContent().trim().isEmpty()) {
					parts.add(
							markdownToPdfService.renderMarkdownToPdf(md.getContent(), md.isLandscape(), activityName));
				}
			}
		}
		return parts;
	}

	/**
	 * Build ordered markdown/landscape lists for DOCX merged rendering. Order:
	 * Deckblatt, Artikulationsschema, Hintergrundwissen.
	 */
	private void buildOrderedDocxParts(ActivityResponse activity, List<String> markdowns, List<Boolean> landscapes) {

		if (activity.getMarkdowns() == null) {
			return;
		}
		for (String type : MARKDOWN_TYPE_ORDER) {
			for (MarkdownResponse md : activity.getMarkdowns()) {
				if (type.equals(md.getType()) && md.getContent() != null && !md.getContent().trim().isEmpty()) {
					markdowns.add(md.getContent());
					landscapes.add(md.isLandscape());
				}
			}
		}
		logger.debug("Ordered DOCX parts - Markdowns: {}, Landscapes: {}", markdowns, landscapes);
	}

	/**
	 * Parse and validate the "documentId" field from a request body map.
	 */
	private UUID parseRequiredDocumentId(Map<String, Object> request) {
		Object documentIdObj = request.get("documentId");
		if (documentIdObj == null) {
			throw new IllegalArgumentException("documentId is required");
		}
		try {
			return UUID.fromString(documentIdObj.toString());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid documentId format");
		}
	}

	/**
	 * Build a file download response with appropriate headers.
	 */
	private ResponseEntity<byte[]> buildFileDownloadResponse(byte[] content, String name, String extension,
			MediaType mediaType, String disposition) {
		String downloadName = sanitizeDownloadFilename(name != null ? name : "activity") + extension;
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(mediaType);
		headers.setContentDispositionFormData(disposition, downloadName);
		headers.setContentLength(content.length);
		return ResponseEntity.ok().headers(headers).body(content);
	}

	private String sanitizeDownloadFilename(String name) {
		if (name == null || name.isBlank()) {
			return "activity";
		}
		String sanitized = name.replaceAll("[^a-zA-Z0-9._\\- ]", "_").trim();
		return sanitized.isEmpty() ? "activity" : sanitized;
	}
}
