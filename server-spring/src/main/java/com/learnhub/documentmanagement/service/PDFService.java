package com.learnhub.documentmanagement.service;

import com.learnhub.activitymanagement.dto.response.LessonPlanInfoResponse;
import com.learnhub.documentmanagement.entity.PDFDocument;
import com.learnhub.activitymanagement.entity.Activity;
import com.learnhub.documentmanagement.repository.PDFDocumentRepository;
import com.learnhub.activitymanagement.repository.ActivityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PDFService {

    private static final Logger logger = LoggerFactory.getLogger(PDFService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private PDFDocumentRepository pdfDocumentRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private LLMService llmService;

    @Value("${pdf.storage.path:/app/data/pdfs}")
    private String pdfStoragePath;

    @Transactional
    public Long storePdf(byte[] pdfContent, String filename) throws IOException {
        // Ensure storage directory exists
        Path storagePath = Paths.get(pdfStoragePath);
        Files.createDirectories(storagePath);

        // Save PDF to filesystem
        Path filePath = storagePath.resolve(filename);
        Files.write(filePath, pdfContent);

        // Create database record
        PDFDocument document = new PDFDocument();
        document.setFilename(filename);
        document.setFilePath(filePath.toString());
        document.setFileSize((long) pdfContent.length);
        document.setExtractedFields("{}");
        document.setCreatedAt(LocalDateTime.now());

        document = pdfDocumentRepository.save(document);
        return document.getId();
    }

    public byte[] getPdfContent(Long documentId) throws IOException {
        PDFDocument document = pdfDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("PDF document not found"));

        Path filePath = Paths.get(document.getFilePath());
        if (!Files.exists(filePath)) {
            throw new RuntimeException("PDF file not found on filesystem");
        }

        return Files.readAllBytes(filePath);
    }

    public PDFDocument getPdfDocument(Long documentId) {
        return pdfDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("PDF document not found"));
    }

    @Transactional
    public void updatePdfExtractionResults(Long documentId, Map<String, Object> extractedFields, 
                                           String confidenceScore, String extractionQuality) {
        PDFDocument document = pdfDocumentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("PDF document not found"));
        
        // Convert extracted fields to JSON string
        try {
            String json = extractedFields != null ? OBJECT_MAPPER.writeValueAsString(extractedFields) : "{}";
            document.setExtractedFields(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize extracted fields to JSON", e);
        }
        document.setConfidenceScore(confidenceScore);
        document.setExtractionQuality(extractionQuality);

        pdfDocumentRepository.save(document);
    }

    public LessonPlanInfoResponse getLessonPlanInfo(List<Map<String, Object>> activities) {
        int availablePdfs = 0;
        List<Integer> missingPdfs = new ArrayList<>();

        for (int i = 0; i < activities.size(); i++) {
            Map<String, Object> activity = activities.get(i);
            Object docIdObj = activity.get("document_id");

            if (docIdObj != null) {
                try {
                    Long documentId = Long.parseLong(docIdObj.toString());
                    byte[] content = getPdfContent(documentId);
                    if (content != null && content.length > 0) {
                        availablePdfs++;
                    } else {
                        missingPdfs.add(i);
                    }
                } catch (Exception e) {
                    missingPdfs.add(i);
                }
            } else {
                missingPdfs.add(i);
            }
        }

        boolean canGenerate = missingPdfs.isEmpty();
        return new LessonPlanInfoResponse(canGenerate, availablePdfs, missingPdfs);
    }

    public byte[] generateLessonPlan(List<Map<String, Object>> activities,
            Map<String, Object> searchCriteria,
            List<Map<String, Object>> breaks,
            Integer totalDuration) throws IOException {
        /**
         * Generate a complete lesson plan PDF matching Flask implementation:
         * 1. Generate summary/cover page with search criteria, activities list, breaks
         * 2. Get activity PDFs based on document_id (or fallback to ID 999)
         * 3. Merge summary page + activity PDFs using PDFBox
         */

        if (activities == null || activities.isEmpty()) {
            throw new RuntimeException("No activities provided for lesson plan");
        }

        try {
            // 1. Generate summary page
            byte[] summaryPdf = generateSummaryPage(activities, searchCriteria, breaks, totalDuration);
            logger.info("Generated summary page for lesson plan PDF.");
            // 2. Get activity PDFs
            List<byte[]> activityPdfs = getActivityPdfs(activities);
            logger.info("Retrieved {} activity PDFs for lesson plan.", activityPdfs.size());
            // 3. Merge all PDFs
            return mergePdfs(summaryPdf, activityPdfs);

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error generating lesson plan PDF: {}", e.toString());
            throw new IOException("Failed to generate lesson plan: " + e.getMessage(), e);
        }
    }

    private List<byte[]> getActivityPdfs(List<Map<String, Object>> activities) {
        List<byte[]> pdfs = new ArrayList<>();

        for (Map<String, Object> activityMap : activities) {
            try {
                // Extract activity ID
                Object idObj = activityMap.get("id");
                if (idObj == null)
                    continue;

                Long activityId = Long.parseLong(idObj.toString());

                // Get activity from database
                Activity activity = activityRepository.findById(activityId).orElse(null);
                if (activity == null)
                    continue;

                byte[] pdfContent = null;

                // Try to get PDF via document_id
                if (activity.getDocumentId() != null) {
                    try {
                        pdfContent = getPdfContent(activity.getDocumentId());
                    } catch (Exception e) {
                        // Continue to fallback
                    }
                }

                // FALLBACK: Try PDF ID 999 (same as Flask)
                if (pdfContent == null) {
                    try {
                        pdfContent = getPdfContent(999L);
                    } catch (Exception e) {
                        // No PDF available
                    }
                }

                if (pdfContent != null && pdfContent.length > 0) {
                    pdfs.add(pdfContent);
                }

            } catch (Exception e) {
                // Skip this activity if any error
                continue;
            }
        }

        return pdfs;
    }

    private byte[] mergePdfs(byte[] summaryPdf, List<byte[]> activityPdfs) throws IOException {
        // Build merged document fully in memory without touching disk
        try (PDDocument merged = new PDDocument()) {
            // Add summary pages first
            appendDocumentPages(merged, summaryPdf);

            // Append each activity PDF
            for (byte[] activityPdf : activityPdfs) {
                if (activityPdf != null && activityPdf.length > 0) {
                    appendDocumentPages(merged, activityPdf);
                }
            }

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                merged.save(outputStream);
                return outputStream.toByteArray();
            }
        }
    }

    private void appendDocumentPages(PDDocument target, byte[] sourceBytes) throws IOException {
        // Use PDFMergerUtility for reliable page appending (PDFBox 3.x compatible)
        PDDocument source = Loader.loadPDF(sourceBytes);
        try {
            org.apache.pdfbox.multipdf.PDFMergerUtility merger = new org.apache.pdfbox.multipdf.PDFMergerUtility();
            merger.appendDocument(target, source);
        } finally {
            source.close();
        }
    }

    private byte[] generateSummaryPage(List<Map<String, Object>> activities,
            Map<String, Object> searchCriteria,
            List<Map<String, Object>> breaks,
            Integer totalDuration) throws IOException {
        /**
         * Generate a summary/cover page using PDFBox with table-based layout matching
         * Flask's ReportLab output
         */

        PDDocument document = new PDDocument();

        try {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            try {
                float margin = 50;
                float yPosition = page.getMediaBox().getHeight() - margin;
                float pageWidth = page.getMediaBox().getWidth() - 2 * margin;

                // Title - centered
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
                String title = "Lesson Plan Summary";
                float titleWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD).getStringWidth(title) / 1000
                        * 18;
                contentStream.newLineAtOffset(margin + (pageWidth - titleWidth) / 2, yPosition);
                contentStream.showText(title);
                contentStream.endText();

                yPosition -= 50;

                // Search Criteria Section with Table
                yPosition = drawSectionHeader(contentStream, margin, yPosition, "Search Criteria:");
                yPosition -= 10;

                if (searchCriteria != null && !searchCriteria.isEmpty()) {
                    List<String[]> criteriaRows = new ArrayList<>();
                    for (Map.Entry<String, Object> entry : searchCriteria.entrySet()) {
                        if (entry.getValue() != null && !entry.getValue().toString().isEmpty()) {
                            String key = entry.getKey().replace("_", " ");
                            key = key.substring(0, 1).toUpperCase() + key.substring(1);
                            criteriaRows.add(new String[] { key, entry.getValue().toString() });
                        }
                    }
                    if (!criteriaRows.isEmpty()) {
                        yPosition = drawTable(contentStream, margin, yPosition,
                                new float[] { 150, 350 }, criteriaRows, false);
                    }
                }

                yPosition -= 20;

                // Activities Section with Table
                yPosition = drawSectionHeader(contentStream, margin, yPosition, "Activities:");
                yPosition -= 10;

                List<String[]> activityRows = new ArrayList<>();
                activityRows.add(new String[] { "#", "Name", "Duration", "Format", "Bloom Level" });
                int activityNum = 1;
                for (Map<String, Object> activity : activities) {
                    String name = activity.getOrDefault("name", "N/A").toString();
                    Object durationObj = activity.get("duration_min_minutes");
                    String duration = durationObj != null ? durationObj.toString() + " min" : "N/A";
                    String format = activity.getOrDefault("format", "N/A").toString();
                    String bloomLevel = activity.getOrDefault("bloom_level", "N/A").toString();

                    activityRows.add(new String[] {
                            String.valueOf(activityNum++),
                            name,
                            duration,
                            format,
                            bloomLevel
                    });
                }
                yPosition = drawTable(contentStream, margin, yPosition,
                        new float[] { 30, 200, 80, 80, 110 }, activityRows, true);

                yPosition -= 20;

                // Breaks Section with Table (if applicable)
                if (breaks != null && !breaks.isEmpty()) {
                    yPosition = drawSectionHeader(contentStream, margin, yPosition, "Breaks:");
                    yPosition -= 10;

                    List<String[]> breakRows = new ArrayList<>();
                    breakRows.add(new String[] { "Duration", "Description" });
                    for (Map<String, Object> breakItem : breaks) {
                        Object durationObj = breakItem.get("duration");
                        String duration = durationObj != null ? durationObj.toString() + " min" : "N/A";
                        String description = breakItem.getOrDefault("description", "Break").toString();
                        breakRows.add(new String[] { duration, description });
                    }
                    yPosition = drawTable(contentStream, margin, yPosition,
                            new float[] { 100, 400 }, breakRows, true);

                    yPosition -= 20;
                }

                // Total Duration
                yPosition = drawSectionHeader(contentStream, margin, yPosition,
                        "Total Duration: " + (totalDuration != null ? totalDuration : 0) + " minutes");

                yPosition -= 30;

                // Generated Timestamp
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                contentStream.newLineAtOffset(margin, yPosition);
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:%M:%S"));
                contentStream.showText("Generated: " + timestamp);
                contentStream.endText();

            } finally {
                contentStream.close();
            }

            // Save to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();

        } finally {
            document.close();
        }
    }

    private float drawSectionHeader(PDPageContentStream contentStream, float x, float y, String text)
            throws IOException {
        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
        return y - 20;
    }

    private float drawTable(PDPageContentStream contentStream, float x, float y,
            float[] columnWidths, List<String[]> rows, boolean hasHeader) throws IOException {

        float cellPadding = 5;
        float rowHeight = 20;
        float fontSize = 10;

        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

        float tableWidth = 0;
        for (float width : columnWidths) {
            tableWidth += width;
        }

        float currentY = y;

        for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
            String[] row = rows.get(rowIdx);
            boolean isHeader = hasHeader && rowIdx == 0;

            // Draw cell backgrounds and borders
            float currentX = x;
            for (int colIdx = 0; colIdx < row.length && colIdx < columnWidths.length; colIdx++) {
                // Draw cell background (gray for headers)
                if (isHeader) {
                    contentStream.setNonStrokingColor(0.8f, 0.8f, 0.8f);
                    contentStream.addRect(currentX, currentY - rowHeight, columnWidths[colIdx], rowHeight);
                    contentStream.fill();
                    contentStream.setNonStrokingColor(0f, 0f, 0f);
                }

                // Draw cell border
                contentStream.setStrokingColor(0f, 0f, 0f);
                contentStream.addRect(currentX, currentY - rowHeight, columnWidths[colIdx], rowHeight);
                contentStream.stroke();

                currentX += columnWidths[colIdx];
            }

            // Draw cell text
            currentX = x;
            for (int colIdx = 0; colIdx < row.length && colIdx < columnWidths.length; colIdx++) {
                String cellText = row[colIdx];
                if (cellText == null)
                    cellText = "";

                // Truncate text if too long
                if (cellText.length() > 40) {
                    cellText = cellText.substring(0, 37) + "...";
                }

                contentStream.beginText();
                contentStream.setFont(isHeader ? boldFont : font, fontSize);
                contentStream.newLineAtOffset(currentX + cellPadding, currentY - rowHeight + cellPadding + 2);
                contentStream.showText(cellText);
                contentStream.endText();

                currentX += columnWidths[colIdx];
            }

            currentY -= rowHeight;
        }

        return currentY;
    }

    /**
     * Extract activity data from PDF content using LLM.
     * Matches Flask's PDFProcessor.parse_pdf_content() behavior.
     */
    public Map<String, Object> extractActivityFromPdf(byte[] pdfContent, Long documentId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Extract text from PDF using PDFBox
            String text;
            try (PDDocument document = Loader.loadPDF(pdfContent)) {
                org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
                text = stripper.getText(document);
            }

            // Check if text is sufficient
            if (text == null || text.trim().length() < 10) {
                logger.warn("PDF contains insufficient text for extraction");
                result.put("error", "PDF contains insufficient text for extraction");
                result.put("confidence", null);
                return result;
            }

            // Extract activity data using LLM
            Map<String, Object> extractionResult = llmService.extractActivityData(text);

            // Get confidence score
            Double confidence = extractionResult.containsKey("confidence")
                    ? (Double) extractionResult.get("confidence")
                    : null;

            // Get extracted data
            Map<String, Object> data = extractionResult.containsKey("data")
                    ? (Map<String, Object>) extractionResult.get("data")
                    : new HashMap<>();

            // Assess extraction quality
            String quality = assessExtractionQuality(data, confidence);

            // Build result
            result.put("data", data);
            result.put("confidence", confidence);
            result.put("text_length", text.length());
            result.put("extraction_quality", quality);

            return result;

        } catch (Exception e) {
            logger.error("Failed to extract activity data from PDF: {}", e.getMessage());
            result.put("error", "Failed to parse PDF: " + e.getMessage());
            result.put("confidence", null);
            return result;
        }
    }

    /**
     * Assess the quality of extraction based on field completeness and confidence.
     * Matches Flask's PDFProcessor._assess_extraction_quality() logic.
     */
    private String assessExtractionQuality(Map<String, Object> data, Double confidence) {
        if (data == null || data.isEmpty()) {
            return "low";
        }

        // Count how many important fields were extracted
        int extractedFields = 0;
        String[] importantFields = { "name", "age_min", "age_max", "format", "bloom_level",
                "duration_min_minutes", "topics" };

        for (String field : importantFields) {
            if (data.containsKey(field) && data.get(field) != null) {
                extractedFields++;
            }
        }

        // Determine quality level based on field completeness and confidence
        if (extractedFields >= 5 && confidence != null && confidence >= 0.7) {
            return "high";
        } else if (extractedFields >= 3 && confidence != null && confidence >= 0.5) {
            return "medium";
        } else {
            return "low";
        }
    }
}
