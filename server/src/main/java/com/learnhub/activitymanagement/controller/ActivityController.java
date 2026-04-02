package com.learnhub.activitymanagement.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhub.activitymanagement.dto.request.ActivityUpsertRequest;
import com.learnhub.activitymanagement.dto.request.ActivityFilterRequest;
import com.learnhub.activitymanagement.dto.request.DocumentIdRequest;
import com.learnhub.activitymanagement.dto.request.GenerateMarkdownsRequest;
import com.learnhub.activitymanagement.dto.request.LessonPlanRequest;
import com.learnhub.activitymanagement.dto.request.RecommendationRequest;
import com.learnhub.activitymanagement.dto.response.ActivitiesListResponse;
import com.learnhub.activitymanagement.dto.response.ActivityMutationResponse;
import com.learnhub.activitymanagement.dto.response.ActivityResponse;
import com.learnhub.activitymanagement.dto.response.GenerateMarkdownsResponse;
import com.learnhub.activitymanagement.dto.response.LessonPlanInfoResponse;
import com.learnhub.activitymanagement.dto.response.MessageResponse;
import com.learnhub.activitymanagement.dto.response.MarkdownResponse;
import com.learnhub.activitymanagement.dto.response.MetadataExtractionResponse;
import com.learnhub.activitymanagement.dto.response.RecommendationsResponse;
import com.learnhub.activitymanagement.service.ActivityService;
import com.learnhub.activitymanagement.service.RecommendationService;
import com.learnhub.documentmanagement.service.LLMService;
import com.learnhub.documentmanagement.service.MarkdownToDocxService;
import com.learnhub.documentmanagement.service.MarkdownToPdfService;
import com.learnhub.documentmanagement.service.PDFService;
import com.learnhub.exception.ResourceNotFoundException;
import com.learnhub.usermanagement.service.UserSearchHistoryService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/activities")
@Tag(name = "Activities", description = "Activity management and recommendations endpoints")
public class ActivityController {

	private static final Logger logger = LoggerFactory.getLogger(ActivityController.class);
	private static final String[] MARKDOWN_TYPE_ORDER = {"deckblatt", "artikulationsschema", "hintergrundwissen"};
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
	public ResponseEntity<ActivitiesListResponse> getActivities(@ModelAttribute ActivityFilterRequest request,
			Authentication authentication) {
		logger.info(
				"GET /api/activities/ - Get activities called with filters: name={}, ageMin={}, ageMax={}, format={}, limit={}, offset={}",
				request.name(), request.ageMin(), request.ageMax(), request.format(), request.limit(),
				request.offset());
		boolean includeSourcePdf = isAdmin(authentication);
		List<ActivityResponse> activities = activityService.getActivitiesWithFilters(request.name(), request.ageMin(),
				request.ageMax(), request.durationMin(), request.durationMax(), request.format(), request.bloomLevel(),
				request.mentalLoad(), request.physicalEnergy(), request.resourcesNeeded(), request.topics(),
				request.limit(), request.offset(), includeSourcePdf);
		ActivitiesListResponse response = new ActivitiesListResponse(
				activityService.countActivitiesWithFilters(request.name(), request.ageMin(), request.ageMax(),
						request.durationMin(), request.durationMax(), request.format(), request.bloomLevel(),
						request.mentalLoad(), request.physicalEnergy(), request.resourcesNeeded(), request.topics()),
				activities, request.limit(), request.offset());
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Get activity by ID", description = "Get a single activity by its ID")
	public ResponseEntity<ActivityResponse> getActivity(@PathVariable UUID id, Authentication authentication) {
		logger.info("GET /api/activities/{} - Get activity by ID called", id);
		ActivityResponse activity = activityService.getActivityById(id, isAdmin(authentication));
		return ResponseEntity.ok(activity);
	}

	@PostMapping("/create")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Create activity", description = "Create a new activity (admin only)")
	public ResponseEntity<ActivityMutationResponse> createActivity(@RequestBody ActivityUpsertRequest request) {
		logger.info("POST /api/activities/create - Create activity called");
		ActivityResponse saved = activityService.createActivityWithValidation(toMap(request));
		logger.info("POST /api/activities/create - Activity created with id={}", saved.getId());
		return ResponseEntity.status(201).body(new ActivityMutationResponse(saved));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Delete activity", description = "Delete an activity by its ID (admin only)")
	public ResponseEntity<MessageResponse> deleteActivity(@PathVariable UUID id) {
		logger.info("DELETE /api/activities/{} - Delete activity called", id);
		activityService.deleteActivity(id);
		logger.info("DELETE /api/activities/{} - Activity deleted successfully", id);
		return ResponseEntity.ok(new MessageResponse("Activity deleted successfully"));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Update activity", description = "Update an existing activity's metadata and artikulationsschema (admin only)")
	public ResponseEntity<ActivityResponse> updateActivity(@PathVariable UUID id,
			@RequestBody ActivityUpsertRequest request) {
		logger.info("PUT /api/activities/{} - Update activity called", id);
		ActivityResponse updated = activityService.updateActivityFromMap(id, toMap(request));
		logger.info("PUT /api/activities/{} - Activity updated successfully", id);
		return ResponseEntity.ok(updated);
	}

	@GetMapping("/recommendations")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Get activity recommendations", description = "Get personalized activity recommendations with scoring")
	public ResponseEntity<RecommendationsResponse> getRecommendations(@ModelAttribute RecommendationRequest request,
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
		RecommendationsResponse response = recommendationService.getRecommendations(criteria, request.includeBreaks(),
				request.maxActivityCount(), request.limit());

		return ResponseEntity.ok(response);
	}

	@PostMapping("/lesson-plan")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Generate lesson plan", description = "Generate a lesson plan from selected activities")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Lesson plan PDF", content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary"))) })
	public ResponseEntity<byte[]> generateLessonPlan(@RequestBody LessonPlanRequest request) throws IOException {
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

	@PostMapping(value = "/upload-pdf-draft", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Upload PDF for draft activity", description = "Upload a PDF and cache it, returning a document_id and extracted metadata for the 2-step creation flow (admin only)")
	public ResponseEntity<MetadataExtractionResponse> uploadPdfDraft(@RequestParam("pdf_file") MultipartFile pdfFile,
			@RequestParam(value = "extractMetadata", defaultValue = "true") boolean extractMetadata) {
		logger.info("POST /api/activities/upload-pdf-draft - Upload PDF draft called with file={}",
				pdfFile.getOriginalFilename());
		Map<String, Object> result = activityService.uploadPdfAndExtractMetadata(pdfFile, extractMetadata);
		return ResponseEntity.status(201).body(toMetadataExtractionResponse(result));
	}

	@PostMapping("/regenerate-metadata")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Regenerate metadata", description = "Re-run metadata extraction for a cached or persisted PDF (admin only)")
	public ResponseEntity<MetadataExtractionResponse> regenerateMetadata(@RequestBody DocumentIdRequest request) {
		logger.info("POST /api/activities/regenerate-metadata called");
		UUID documentId = parseRequiredDocumentId(request.getDocumentId());
		Map<String, Object> result = activityService.extractMetadataFromDocument(documentId);
		return ResponseEntity.ok(toMetadataExtractionResponse(result));
	}

	@PostMapping("/generate-markdowns")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Generate all activity markdowns", description = "Generate Deckblatt, Artikulationsschema, and Hintergrundwissen markdowns from an uploaded PDF (admin only)")
	public ResponseEntity<GenerateMarkdownsResponse> generateActivityMarkdowns(
			@RequestBody GenerateMarkdownsRequest request) {
		logger.info("POST /api/activities/generate-markdowns called");
		UUID documentId = parseRequiredDocumentId(request.getDocumentId());

		String pdfText = pdfService.extractTextFromPdf(documentId);
		if (pdfText == null || pdfText.trim().length() < 10) {
			throw new IllegalArgumentException("PDF does not contain sufficient text for generation");
		}

		Map<String, Object> metadata = request.getMetadata();
		List<String> types = request.getTypes();
		GenerateMarkdownsResponse response = new GenerateMarkdownsResponse();
		response.setDocumentId(documentId.toString());

		boolean generateAll = types == null || types.isEmpty();

		if (generateAll || types.contains("deckblatt")) {
			String deckblatt = llmService.generateDeckblatt(pdfText, metadata);
			response.setDeckblattMarkdown(deckblatt);
		}

		if (generateAll || types.contains("artikulationsschema")) {
			String artikulationsschema = llmService.generateArtikulationsschema(pdfText, metadata);
			response.setArtikulationsschemaMarkdown(artikulationsschema);
		}

		if (generateAll || types.contains("hintergrundwissen")) {
			String hintergrundwissen = llmService.generateHintergrundwissen(pdfText, metadata);
			response.setHintergrundwissenMarkdown(hintergrundwissen);
		}

		return ResponseEntity.ok(response);
	}

	@GetMapping("/{activityId}/pdf")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Download activity as combined PDF", description = "Download all markdown files (Deckblatt portrait, Artikulationsschema landscape, Hintergrundwissen portrait) as a single PDF")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Combined activity PDF", content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary"))) })
	public ResponseEntity<byte[]> downloadActivityPdf(@PathVariable UUID activityId) {
		logger.info("GET /api/activities/{}/pdf - Download combined activity PDF", activityId);
		ActivityResponse activity = activityService.getActivityById(activityId);
		List<byte[]> pdfParts = buildOrderedPdfParts(activity);

		if (pdfParts.isEmpty()) {
			throw new ResourceNotFoundException("No markdown content available for this activity");
		}

		byte[] pdfBytes = pdfParts.size() == 1 ? pdfParts.get(0) : markdownToPdfService.mergePdfs(pdfParts);

		return buildFileDownloadResponse(pdfBytes, activity.getName(), ".pdf", MediaType.APPLICATION_PDF, "inline");
	}

	@GetMapping("/{activityId}/docx")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Download activity as combined DOCX", description = "Download all markdown files (Deckblatt portrait, Artikulationsschema landscape, Hintergrundwissen portrait) as a single DOCX")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Combined activity DOCX", content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document", schema = @Schema(type = "string", format = "binary"))) })
	public ResponseEntity<byte[]> downloadActivityDocx(@PathVariable UUID activityId) {
		logger.info("GET /api/activities/{}/docx - Download combined activity DOCX", activityId);
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

	private UUID parseRequiredDocumentId(UUID documentId) {
		if (documentId == null) {
			throw new IllegalArgumentException("documentId is required");
		}
		return documentId;
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

	private boolean isAdmin(Authentication authentication) {
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
	}

	private Map<String, Object> toMap(ActivityUpsertRequest request) {
		return OBJECT_MAPPER.convertValue(request, new TypeReference<Map<String, Object>>() {
		});
	}

	@SuppressWarnings("unchecked")
	private MetadataExtractionResponse toMetadataExtractionResponse(Map<String, Object> result) {
		Map<String, Object> extractedData = result.get("extractedData") instanceof Map
				? (Map<String, Object>) result.get("extractedData")
				: Map.of();
		double extractionConfidence = result.get("extractionConfidence") instanceof Number
				? ((Number) result.get("extractionConfidence")).doubleValue()
				: 0.0;
		String documentId = result.get("documentId") != null ? result.get("documentId").toString() : null;
		String extractionQuality = result.get("extractionQuality") != null ? result.get("extractionQuality").toString()
				: null;
		return new MetadataExtractionResponse(documentId, extractedData, extractionConfidence, extractionQuality);
	}
}
