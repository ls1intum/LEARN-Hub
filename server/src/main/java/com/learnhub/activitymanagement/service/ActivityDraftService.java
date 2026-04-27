package com.learnhub.activitymanagement.service;

import com.learnhub.activitymanagement.dto.response.ActivityResponse;
import com.learnhub.activitymanagement.entity.Activity;
import com.learnhub.activitymanagement.entity.enums.ActivityFormat;
import com.learnhub.activitymanagement.entity.enums.ActivityStatus;
import com.learnhub.activitymanagement.entity.enums.BloomLevel;
import com.learnhub.activitymanagement.entity.enums.EnergyLevel;
import com.learnhub.activitymanagement.repository.ActivityRepository;
import com.learnhub.activitymanagement.entity.enums.DocumentType;
import com.learnhub.documentmanagement.entity.PDFDocument;
import com.learnhub.documentmanagement.repository.PDFDocumentRepository;
import com.learnhub.documentmanagement.service.LLMService;
import com.learnhub.documentmanagement.service.PDFService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    @Qualifier("markdownGenerationExecutor")
    private ExecutorService markdownGenerationExecutor;

    /**
     * Validates and persists the PDF, then creates either:
     * - a PENDING activity with background generation, or
     * - a DRAFT activity immediately when generation is skipped.
     */
    @Transactional
    public ActivityResponse initiateDraftCreation(MultipartFile pdfFile, boolean generateContent) {
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
        String displayName = filename.endsWith(".pdf")
                ? filename.substring(0, filename.length() - 4)
                : filename;
        // Truncate to DB column length
        if (displayName.length() > 255) displayName = displayName.substring(0, 255);

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
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    CompletableFuture.runAsync(
                            () -> generateActivityContent(activityId, bgDocumentId), markdownGenerationExecutor);
                }
            });
        }

        return activityService.convertToResponse(saved, true);
    }

    private void generateActivityContent(UUID activityId, UUID documentId) {
        logger.info("Background generation started for activity {}", activityId);
        try {
            // Step 1: extract metadata
            Map<String, Object> extractionResult = extractionService.extractMetadataFromDocument(documentId);
            @SuppressWarnings("unchecked")
            Map<String, Object> extractedData = extractionResult.get("extractedData") instanceof Map
                    ? (Map<String, Object>) extractionResult.get("extractedData")
                    : Map.of();
            Map<String, Object> withDefaults = extractionService.applyActivityDefaults(new HashMap<>(extractedData));
            activityService.updateActivityWithMetadata(activityId, withDefaults);

            // Step 2: generate markdowns
            String pdfText = pdfService.extractTextFromPdf(documentId);
            if (pdfText == null || pdfText.trim().length() < 10) {
                throw new IllegalStateException("PDF does not contain sufficient text for markdown generation");
            }

            List<byte[]> pdfPageImages = llmService.isVisionEnabled()
                    ? pdfService.renderPdfPagesAsImages(documentId)
                    : null;

            Map<String, String> markdowns = generateAllMarkdowns(pdfText, withDefaults, pdfPageImages);
            activityService.addMarkdownsToActivity(activityId, markdowns);

            // Step 3: mark as DRAFT
            activityService.setActivityStatus(activityId, ActivityStatus.DRAFT);
            logger.info("Background generation finished for activity {} → DRAFT", activityId);
        } catch (Exception e) {
            logger.error("Background generation failed for activity {}: {}", activityId, e.getMessage(), e);
            activityService.setActivityGenerationError(activityId, e.getMessage());
        }
    }

    private Map<String, String> generateAllMarkdowns(String pdfText, Map<String, Object> metadata,
            List<byte[]> pdfPageImages) {

        Map<String, String> result = new HashMap<>();
        if (pdfPageImages != null) {
            // Vision mode: sequential
            result.put("deckblatt", llmService.generateDeckblatt(pdfText, metadata));
            result.put("artikulationsschema", llmService.generateArtikulationsschema(pdfText, metadata));
            result.put("hintergrundwissen", llmService.generateHintergrundwissen(pdfText, metadata));
            Map<String, String> uebung = llmService.generateUebungAndLoesung(pdfText, metadata, pdfPageImages);
            result.put("uebung", uebung.get("uebung"));
            result.put("uebung_loesung", uebung.get("uebung_loesung"));
        } else {
            // Text-only mode: parallel
            CompletableFuture<String> deckblattF = CompletableFuture.supplyAsync(
                    () -> llmService.generateDeckblatt(pdfText, metadata), markdownGenerationExecutor);
            CompletableFuture<String> artikF = CompletableFuture.supplyAsync(
                    () -> llmService.generateArtikulationsschema(pdfText, metadata), markdownGenerationExecutor);
            CompletableFuture<String> hinterF = CompletableFuture.supplyAsync(
                    () -> llmService.generateHintergrundwissen(pdfText, metadata), markdownGenerationExecutor);
            CompletableFuture<Map<String, String>> uebungF = CompletableFuture.supplyAsync(
                    () -> llmService.generateUebungAndLoesung(pdfText, metadata), markdownGenerationExecutor);

            CompletableFuture.allOf(deckblattF, artikF, hinterF, uebungF).join();

            result.put("deckblatt", deckblattF.join());
            result.put("artikulationsschema", artikF.join());
            result.put("hintergrundwissen", hinterF.join());
            Map<String, String> uebung = uebungF.join();
            result.put("uebung", uebung.get("uebung"));
            result.put("uebung_loesung", uebung.get("uebung_loesung"));
        }
        return result;
    }
}
