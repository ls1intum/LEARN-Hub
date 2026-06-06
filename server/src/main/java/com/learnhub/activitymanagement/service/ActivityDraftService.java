package com.learnhub.activitymanagement.service;

import com.learnhub.activitymanagement.dto.response.ActivityResponse;
import com.learnhub.activitymanagement.entity.Activity;
import com.learnhub.activitymanagement.entity.enums.ActivityFormat;
import com.learnhub.activitymanagement.entity.enums.ActivityStatus;
import com.learnhub.activitymanagement.entity.enums.BloomLevel;
import com.learnhub.activitymanagement.entity.enums.DocumentType;
import com.learnhub.activitymanagement.entity.enums.EnergyLevel;
import com.learnhub.activitymanagement.repository.ActivityRepository;
import com.learnhub.documentmanagement.entity.PDFDocument;
import com.learnhub.documentmanagement.repository.PDFDocumentRepository;
import com.learnhub.documentmanagement.service.LLMService;
import com.learnhub.documentmanagement.service.PDFService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ActivityDraftService {

	private static final Logger logger = LoggerFactory.getLogger(ActivityDraftService.class);
	private static final long MAX_PDF_SIZE_BYTES = 1024 * 1024; // 1 MB

	@Autowired
	private ActivityRepository activityRepository;

	@Autowired
	private PDFDocumentRepository pdfDocumentRepository;

	@Autowired
	private PDFService pdfService;

	@Autowired
	private LLMService llmService;

	@Autowired
	private ActivityExtractionService extractionService;

	@Autowired
	private ActivityService activityService;

	@Autowired
	private DraftSseService draftSseService;

	@Autowired
	@Qualifier("markdownGenerationExecutor")
	private ExecutorService markdownGenerationExecutor;

	/**
	 * Validates and persists the PDF, then creates either: - a PENDING activity
	 * with background generation (when at least one step is enabled), or - a DRAFT
	 * activity immediately when both steps are skipped.
	 */
	@Transactional
	public ActivityResponse initiateDraftCreation(MultipartFile pdfFile, boolean generateMetadata,
			List<String> markdownTypes) {
		boolean generateContent = generateMetadata || !markdownTypes.isEmpty();
		if (pdfFile == null || pdfFile.isEmpty()) {
			throw new IllegalArgumentException("No PDF file provided");
		}
		String filename = pdfFile.getOriginalFilename();
		if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
			throw new IllegalArgumentException("File must be a PDF");
		}
		if (pdfFile.getSize() > MAX_PDF_SIZE_BYTES) {
			throw new IllegalArgumentException(
					"PDF exceeds maximum allowed size of 1 MB (uploaded: " + (pdfFile.getSize() / 1024) + " KB)");
		}

		byte[] pdfContent;
		try {
			pdfContent = pdfFile.getBytes();
		} catch (Exception e) {
			throw new RuntimeException("Failed to read PDF file", e);
		}

		UUID cacheKey = pdfService.cachePdf(pdfContent, filename);
		UUID documentId;
		try {
			documentId = pdfService.finalizePdf(cacheKey);
		} catch (java.io.IOException e) {
			throw new RuntimeException("Failed to persist PDF file", e);
		}

		PDFDocument doc = pdfDocumentRepository.findById(documentId)
				.orElseThrow(() -> new RuntimeException("Could not find persisted PDF document"));
		doc.setType(DocumentType.SOURCE_PDF);
		pdfDocumentRepository.save(doc);

		// Derive a display name from the filename (strip .pdf extension)
		String displayName = filename.endsWith(".pdf") ? filename.substring(0, filename.length() - 4) : filename;
		// Truncate to DB column length
		if (displayName.length() > 255)
			displayName = displayName.substring(0, 255);

		Activity activity = new Activity();
		activity.setName(displayName);
		activity.setDescription("Wird generiert...");
		activity.setAgeMin(6);
		activity.setAgeMax(12);
		activity.setFormat(ActivityFormat.UNPLUGGED);
		activity.setBloomLevel(BloomLevel.REMEMBER);
		activity.setDurationMinMinutes(15);
		activity.setMentalLoad(EnergyLevel.MEDIUM);
		activity.setPhysicalEnergy(EnergyLevel.MEDIUM);
		activity.setPrepTimeMinutes(5);
		activity.setCleanupTimeMinutes(5);
		activity.setResourcesNeeded(new ArrayList<>());
		activity.setTopics(new ArrayList<>());
		activity.setStatus(generateContent ? ActivityStatus.PENDING : ActivityStatus.DRAFT);
		activity.getDocuments().add(doc);

		Activity saved = activityRepository.save(activity);
		UUID activityId = saved.getId();

		if (generateContent) {
			// Schedule background generation to start AFTER the transaction commits so
			// the PDFDocument and Activity rows are visible to the background thread.
			final UUID bgDocumentId = documentId;
			final boolean bgGenerateMetadata = generateMetadata;
			final Set<String> bgMarkdownTypes = new HashSet<>(markdownTypes);
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					CompletableFuture.runAsync(() -> generateActivityContent(activityId, bgDocumentId,
							bgGenerateMetadata, bgMarkdownTypes), markdownGenerationExecutor);
				}
			});
		}

		return activityService.convertToResponse(saved, true);
	}

	private void generateActivityContent(UUID activityId, UUID documentId, boolean generateMetadata,
			Set<String> markdownTypes) {
		logger.info("Background generation started for activity {} (metadata={}, markdownTypes={})", activityId,
				generateMetadata, markdownTypes);
		try {
			Map<String, Object> withDefaults = new HashMap<>();

			if (generateMetadata) {
				Map<String, Object> extractionResult = extractionService.extractMetadataFromDocument(documentId);
				@SuppressWarnings("unchecked")
				Map<String, Object> extractedData = extractionResult.get("extractedData") instanceof Map
						? (Map<String, Object>) extractionResult.get("extractedData")
						: Map.of();
				withDefaults = extractionService.applyActivityDefaults(new HashMap<>(extractedData));
				activityService.updateActivityWithMetadata(activityId, withDefaults);
			}

			if (!markdownTypes.isEmpty()) {
				String pdfText = pdfService.extractTextFromPdf(documentId);
				if (pdfText == null || pdfText.trim().length() < 10) {
					throw new IllegalStateException("PDF does not contain sufficient text for markdown generation");
				}

				List<byte[]> pdfPageImages = llmService.isVisionEnabled()
						? pdfService.renderPdfPagesAsImages(documentId)
						: null;

				Map<String, String> markdowns = generateSelectedMarkdowns(pdfText, withDefaults, pdfPageImages,
						markdownTypes);
				activityService.addMarkdownsToActivity(activityId, markdowns);
			}

			activityService.setActivityStatus(activityId, ActivityStatus.DRAFT);
			draftSseService.sendDraftUpdate(activityId, ActivityStatus.DRAFT, null);
			logger.info("Background generation finished for activity {} → DRAFT", activityId);
		} catch (Exception e) {
			logger.error("Background generation failed for activity {}: {}", activityId, e.getMessage(), e);
			activityService.setActivityGenerationError(activityId, e.getMessage());
			draftSseService.sendDraftUpdate(activityId, ActivityStatus.PENDING, e.getMessage());
		}
	}

	private Map<String, String> generateSelectedMarkdowns(String pdfText, Map<String, Object> metadata,
			List<byte[]> pdfPageImages, Set<String> types) {

		// tafelbild requires artikulationsschema as intermediate input even if artik is
		// not stored
		boolean needArtik = types.contains("artikulationsschema") || types.contains("tafelbild");
		boolean needUebung = types.contains("uebung") || types.contains("uebung_loesung");

		Map<String, String> result = new HashMap<>();
		if (pdfPageImages != null) {
			// Vision mode: sequential
			if (types.contains("deckblatt"))
				result.put("deckblatt", llmService.generateDeckblatt(pdfText, metadata));
			if (needArtik) {
				String artik = llmService.generateArtikulationsschema(pdfText, metadata);
				if (types.contains("artikulationsschema"))
					result.put("artikulationsschema", artik);
				if (types.contains("tafelbild"))
					result.put("tafelbild", llmService.generateTafelbildMarkdown(artik, metadata));
			}
			if (types.contains("hintergrundwissen"))
				result.put("hintergrundwissen", llmService.generateHintergrundwissen(pdfText, metadata));
			if (needUebung) {
				Map<String, String> uebung = llmService.generateUebungAndLoesung(pdfText, metadata, pdfPageImages);
				if (types.contains("uebung"))
					result.put("uebung", uebung.get("uebung"));
				if (types.contains("uebung_loesung"))
					result.put("uebung_loesung", uebung.get("uebung_loesung"));
			}
		} else {
			// Text-only mode: parallel, tafelbild chains off artikulationsschema
			CompletableFuture<String> deckblattF = types.contains("deckblatt")
					? CompletableFuture.supplyAsync(() -> llmService.generateDeckblatt(pdfText, metadata),
							markdownGenerationExecutor)
					: CompletableFuture.completedFuture(null);
			CompletableFuture<String> artikF = needArtik
					? CompletableFuture.supplyAsync(() -> llmService.generateArtikulationsschema(pdfText, metadata),
							markdownGenerationExecutor)
					: CompletableFuture.completedFuture(null);
			CompletableFuture<String> tafelbildF = types.contains("tafelbild")
					? artikF.thenApplyAsync(artik -> llmService.generateTafelbildMarkdown(artik, metadata),
							markdownGenerationExecutor)
					: CompletableFuture.completedFuture(null);
			CompletableFuture<String> hinterF = types.contains("hintergrundwissen")
					? CompletableFuture.supplyAsync(() -> llmService.generateHintergrundwissen(pdfText, metadata),
							markdownGenerationExecutor)
					: CompletableFuture.completedFuture(null);
			CompletableFuture<Map<String, String>> uebungF = needUebung
					? CompletableFuture.supplyAsync(() -> llmService.generateUebungAndLoesung(pdfText, metadata),
							markdownGenerationExecutor)
					: CompletableFuture.completedFuture(null);

			CompletableFuture.allOf(deckblattF, tafelbildF, hinterF, uebungF).join();

			if (types.contains("deckblatt") && deckblattF.join() != null)
				result.put("deckblatt", deckblattF.join());
			if (types.contains("artikulationsschema") && artikF.join() != null)
				result.put("artikulationsschema", artikF.join());
			if (types.contains("tafelbild") && tafelbildF.join() != null)
				result.put("tafelbild", tafelbildF.join());
			if (types.contains("hintergrundwissen") && hinterF.join() != null)
				result.put("hintergrundwissen", hinterF.join());
			if (needUebung && uebungF.join() != null) {
				Map<String, String> uebung = uebungF.join();
				if (types.contains("uebung"))
					result.put("uebung", uebung.get("uebung"));
				if (types.contains("uebung_loesung"))
					result.put("uebung_loesung", uebung.get("uebung_loesung"));
			}
		}
		return result;
	}
}
