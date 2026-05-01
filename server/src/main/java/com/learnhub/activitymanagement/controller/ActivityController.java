package com.learnhub.activitymanagement.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhub.activitymanagement.dto.request.ActivityFilterRequest;
import com.learnhub.activitymanagement.dto.request.ActivityUpsertRequest;
import com.learnhub.activitymanagement.dto.request.DocumentIdRequest;
import com.learnhub.activitymanagement.dto.request.GenerateMarkdownsRequest;
import com.learnhub.activitymanagement.dto.request.LessonPlanRequest;
import com.learnhub.activitymanagement.dto.request.RecommendationRequest;
import com.learnhub.activitymanagement.dto.response.ActivitiesListResponse;
import com.learnhub.activitymanagement.dto.response.ActivityResponse;
import com.learnhub.activitymanagement.dto.response.GenerateMarkdownsResponse;
import com.learnhub.activitymanagement.dto.response.LessonPlanInfoResponse;
import com.learnhub.activitymanagement.dto.response.MarkdownResponse;
import com.learnhub.activitymanagement.dto.response.MessageResponse;
import com.learnhub.activitymanagement.dto.response.MetadataExtractionResponse;
import com.learnhub.activitymanagement.dto.response.RecommendationsResponse;
import com.learnhub.activitymanagement.service.ActivityDraftService;
import com.learnhub.activitymanagement.service.ActivityService;
import com.learnhub.activitymanagement.service.RecommendationService;
import com.learnhub.documentmanagement.service.LLMService;
import com.learnhub.documentmanagement.service.MarkdownToDocxService;
import com.learnhub.documentmanagement.service.MarkdownToPdfService;
import com.learnhub.documentmanagement.service.PDFService;
import com.learnhub.exception.ResourceNotFoundException;
import com.learnhub.security.CurrentUser;
import com.learnhub.usermanagement.service.UserSearchHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
	private static final String[] MARKDOWN_TYPE_ORDER = {"deckblatt", "artikulationsschema", "hintergrundwissen",
			"uebung", "uebung_loesung"};
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Autowired
	private ActivityService activityService;

	@Autowired
	private ActivityDraftService activityDraftService;

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

	@Autowired
	@Qualifier("markdownGenerationExecutor")
	private ExecutorService markdownGenerationExecutor;

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
		UUID userId = CurrentUser.getUserId(authentication);
		List<ActivityResponse> activities = activityService.getActivitiesWithFilters(request.name(), request.ageMin(),
				request.ageMax(), request.durationMin(), request.durationMax(), request.format(), request.bloomLevel(),
				request.mentalLoad(), request.physicalEnergy(), request.resourcesNeeded(), request.topics(),
				request.limit(), request.offset(), includeSourcePdf, userId);
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

	@GetMapping("/{id}/markdowns")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Get activity markdown contents", description = "Get stored markdown contents for editing an activity (admin only)")
	public ResponseEntity<List<MarkdownResponse>> getActivityMarkdowns(@PathVariable UUID id) {
		logger.info("GET /api/activities/{}/markdowns - Get activity markdown contents called", id);
		return ResponseEntity.ok(activityService.getActivityMarkdowns(id));
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
			Authentication authentication) {
		logger.info(
				"GET /api/activities/recommendations - Get recommendations called with targetAge={}, format={}, maxActivityCount={}, limit={}",
				request.targetAge(), request.format(), request.maxActivityCount(), request.limit());
		// Build criteria map using service
		Map<String, Object> criteria = activityService.buildRecommendationCriteria(request.name(), request.targetAge(),
				request.format(), request.bloomLevels(), request.targetDuration(), request.availableResources(),
				request.preferredTopics(), request.priorityCategories());

		// Save search history if user is authenticated
		UUID userId = CurrentUser.getUserId(authentication);
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
			@ApiResponse(responseCode = "200", description = "Lesson plan PDF", content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary")))})
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

		lessonPlanPdf = markdownToPdfService.applyDocumentTitle(lessonPlanPdf, "Lesson Plan");
		return buildFileDownloadResponse(lessonPlanPdf, "lesson_plan", ".pdf", MediaType.APPLICATION_PDF, "inline");
	}

	@PostMapping(value = "/upload-and-create-pending", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Upload PDF and create draft activity", description = "Upload a PDF and create an admin draft. By default the server creates a PENDING activity and starts background metadata/markdown generation. With generateContent=false it creates a DRAFT immediately without background generation. Max file size: 1 MB.")
	public ResponseEntity<ActivityResponse> uploadAndCreatePending(@RequestParam("pdf_file") MultipartFile pdfFile,
			@RequestParam(value = "generateContent", defaultValue = "true") boolean generateContent) {
		logger.info("POST /api/activities/upload-and-create-pending called with file={}, generateContent={}",
				pdfFile.getOriginalFilename(), generateContent);
		ActivityResponse response = activityDraftService.initiateDraftCreation(pdfFile, generateContent);
		return ResponseEntity.status(201).body(response);
	}

	@GetMapping("/drafts")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Get draft activities", description = "Get all PENDING and DRAFT activities (admin only)")
	public ResponseEntity<List<ActivityResponse>> getDraftActivities() {
		logger.info("GET /api/activities/drafts called");
		return ResponseEntity.ok(activityService.getDraftActivities());
	}

	@PutMapping("/{id}/publish")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Publish draft activity", description = "Publish a DRAFT activity so it becomes visible in library and recommendations (admin only)")
	public ResponseEntity<ActivityResponse> publishActivity(@PathVariable UUID id) {
		logger.info("PUT /api/activities/{}/publish called", id);
		return ResponseEntity.ok(activityService.publishActivity(id));
	}

	@PostMapping("/regenerate-image")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Regenerate exercise image", description = "Re-generate a single exercise image with an optional custom prompt (admin only)")
	public ResponseEntity<Map<String, String>> regenerateImage(
			@RequestBody com.learnhub.activitymanagement.dto.request.RegenerateImageRequest request) {
		logger.info("POST /api/activities/regenerate-image called");
		String description = request.getDescription() != null ? request.getDescription().trim() : "";
		String finalDescription = description;
		if (org.springframework.util.StringUtils.hasText(request.getCustomPrompt())) {
			finalDescription = description + (description.isEmpty() ? "" : "\n\nAdditional instructions:\n")
					+ request.getCustomPrompt().trim();
		}
		String contextText = request.getExerciseContext() != null ? request.getExerciseContext() : "";
		String imageMarkdown = llmService.generateImageMarkdown(request.getImageId(), finalDescription, contextText);
		return ResponseEntity.ok(Map.of("imageMarkdown", imageMarkdown));
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

		// Render PDF pages as images once and share across all parallel LLM calls
		List<byte[]> pdfPageImages = llmService.isVisionEnabled()
				? pdfService.renderPdfPagesAsImages(documentId)
				: null;

		Map<String, Object> metadata = request.getMetadata();
		List<String> types = request.getTypes();
		Set<String> requestedTypes = types == null ? Set.of() : new HashSet<>(types);
		GenerateMarkdownsResponse response = new GenerateMarkdownsResponse();
		response.setDocumentId(documentId.toString());

		boolean generateAll = types == null || types.isEmpty();
		boolean useVision = pdfPageImages != null;

		if (useVision) {
			// Vision mode: non-visual generators run as text-only; Uebung/Loesung
			// uses the visual model with images. Run sequentially — local models
			// handle one image request at a time.
			logger.info("Vision mode: generating markdowns sequentially");
			if (generateAll || requestedTypes.contains("deckblatt")) {
				response.setDeckblattMarkdown(llmService.generateDeckblatt(pdfText, metadata));
			}
			if (generateAll || requestedTypes.contains("artikulationsschema")) {
				response.setArtikulationsschemaMarkdown(llmService.generateArtikulationsschema(pdfText, metadata));
			}
			if (generateAll || requestedTypes.contains("hintergrundwissen")) {
				response.setHintergrundwissenMarkdown(llmService.generateHintergrundwissen(pdfText, metadata));
			}
			if (generateAll || requestedTypes.contains("uebung") || requestedTypes.contains("uebung_loesung")) {
				Map<String, String> uebungResult = llmService.generateUebungAndLoesung(pdfText, metadata,
						pdfPageImages);
				response.setUebungMarkdown(uebungResult.get("uebung"));
				response.setUebungLoesungMarkdown(uebungResult.get("uebung_loesung"));
			}
		} else {
			// Text-only mode: run in parallel as before
			CompletableFuture<String> deckblattFuture = (generateAll || requestedTypes.contains("deckblatt"))
					? submitMarkdownGeneration(() -> llmService.generateDeckblatt(pdfText, metadata))
					: null;
			CompletableFuture<String> artikulationsschemaFuture = (generateAll
					|| requestedTypes.contains("artikulationsschema"))
							? submitMarkdownGeneration(() -> llmService.generateArtikulationsschema(pdfText, metadata))
							: null;
			CompletableFuture<String> hintergrundwissenFuture = (generateAll
					|| requestedTypes.contains("hintergrundwissen"))
							? submitMarkdownGeneration(() -> llmService.generateHintergrundwissen(pdfText, metadata))
							: null;
			boolean generateUebung = generateAll || requestedTypes.contains("uebung")
					|| requestedTypes.contains("uebung_loesung");
			CompletableFuture<Map<String, String>> uebungFuture = generateUebung
					? submitMarkdownGeneration(() -> llmService.generateUebungAndLoesung(pdfText, metadata))
					: null;

			List<CompletableFuture<?>> futures = filterNonNull(deckblattFuture, artikulationsschemaFuture,
					hintergrundwissenFuture, uebungFuture);

			try {
				CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
			} catch (CompletionException e) {
				futures.forEach(f -> f.cancel(true));
				throw unwrapCompletionException(e);
			}

			if (deckblattFuture != null) {
				response.setDeckblattMarkdown(deckblattFuture.join());
			}
			if (artikulationsschemaFuture != null) {
				response.setArtikulationsschemaMarkdown(artikulationsschemaFuture.join());
			}
			if (hintergrundwissenFuture != null) {
				response.setHintergrundwissenMarkdown(hintergrundwissenFuture.join());
			}
			if (uebungFuture != null) {
				Map<String, String> uebungResult = uebungFuture.join();
				response.setUebungMarkdown(uebungResult.get("uebung"));
				response.setUebungLoesungMarkdown(uebungResult.get("uebung_loesung"));
			}
		}

		return ResponseEntity.ok(response);
	}

	private <T> CompletableFuture<T> submitMarkdownGeneration(Supplier<T> supplier) {
		return CompletableFuture.supplyAsync(supplier, markdownGenerationExecutor);
	}

	private List<CompletableFuture<?>> filterNonNull(CompletableFuture<?>... futures) {
		return Arrays.stream(futures).filter(Objects::nonNull).toList();
	}

	private RuntimeException unwrapCompletionException(CompletionException e) {
		Throwable cause = e.getCause();
		if (cause instanceof RuntimeException runtimeException) {
			return runtimeException;
		}
		return new RuntimeException("Failed to generate markdowns", cause);
	}

	@GetMapping("/{activityId}/pdf")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Download activity as combined PDF", description = "Download all markdown files (Deckblatt portrait, Artikulationsschema landscape, Hintergrundwissen portrait) as a single PDF")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Combined activity PDF", content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary")))})
	public ResponseEntity<byte[]> downloadActivityPdf(@PathVariable UUID activityId) {
		logger.info("GET /api/activities/{}/pdf - Download combined activity PDF", activityId);
		ActivityResponse activity = activityService.getActivityById(activityId, false, true);
		List<byte[]> pdfParts = buildOrderedPdfParts(activity);

		if (pdfParts.isEmpty()) {
			throw new ResourceNotFoundException("No markdown content available for this activity");
		}

		byte[] pdfBytes = pdfParts.size() == 1 ? pdfParts.get(0) : markdownToPdfService.mergePdfs(pdfParts);
		pdfBytes = markdownToPdfService.applyDocumentTitle(pdfBytes, activity.getName());

		return buildFileDownloadResponse(pdfBytes, activity.getName(), ".pdf", MediaType.APPLICATION_PDF, "inline");
	}

	@GetMapping("/{activityId}/docx")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Download activity as combined DOCX", description = "Download all markdown files (Deckblatt portrait, Artikulationsschema landscape, Hintergrundwissen portrait) as a single DOCX")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Combined activity DOCX", content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document", schema = @Schema(type = "string", format = "binary")))})
	public ResponseEntity<?> downloadActivityDocx(@PathVariable UUID activityId) {
		logger.info("GET /api/activities/{}/docx - Download combined activity DOCX", activityId);
		if (!markdownToDocxService.isAvailable()) {
			return ResponseEntity.status(503).body("DOCX export is not available: Adobe PDF Services credentials are not configured");
		}
		ActivityResponse activity = activityService.getActivityById(activityId, false, true);
		List<String> markdowns = new ArrayList<>();
		List<Boolean> landscapes = new ArrayList<>();
		List<Boolean> exerciseSheets = new ArrayList<>();
		buildOrderedDocxParts(activity, markdowns, landscapes, exerciseSheets);

		if (markdowns.isEmpty()) {
			throw new ResourceNotFoundException("No markdown content available for this activity");
		}

		String activityName = activity.getName() != null ? activity.getName() : "";
		byte[] docxBytes = markdownToDocxService.renderMergedDocx(markdowns, landscapes, exerciseSheets, activityName);

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
					boolean exerciseSheet = "uebung".equals(md.getType());
					parts.add(markdownToPdfService.renderMarkdownToPdf(md.getContent(), md.isLandscape(), activityName,
							exerciseSheet));
				}
			}
		}
		return parts;
	}

	/**
	 * Build ordered markdown/landscape lists for DOCX merged rendering. Order:
	 * Deckblatt, Artikulationsschema, Hintergrundwissen.
	 */
	private void buildOrderedDocxParts(ActivityResponse activity, List<String> markdowns, List<Boolean> landscapes,
			List<Boolean> exerciseSheets) {

		if (activity.getMarkdowns() == null) {
			return;
		}
		for (String type : MARKDOWN_TYPE_ORDER) {
			for (MarkdownResponse md : activity.getMarkdowns()) {
				if (type.equals(md.getType()) && md.getContent() != null && !md.getContent().trim().isEmpty()) {
					markdowns.add(md.getContent());
					landscapes.add(md.isLandscape());
					exerciseSheets.add("uebung".equals(md.getType()));
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
		String extractionQuality = result.get("extractionQuality") != null
				? result.get("extractionQuality").toString()
				: null;
		return new MetadataExtractionResponse(documentId, extractedData, extractionConfidence, extractionQuality);
	}
}
