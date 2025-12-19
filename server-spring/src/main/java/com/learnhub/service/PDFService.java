package com.learnhub.service;

import com.learnhub.dto.response.LessonPlanInfoResponse;
import com.learnhub.model.PDFDocument;
import com.learnhub.model.Activity;
import com.learnhub.repository.PDFDocumentRepository;
import com.learnhub.repository.ActivityRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
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
import java.util.List;
import java.util.Map;

@Service
public class PDFService {

    @Autowired
    private PDFDocumentRepository pdfDocumentRepository;

    @Autowired
    private ActivityRepository activityRepository;

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
        // In a real implementation, use a JSON library like Jackson
        document.setExtractedFields(extractedFields != null ? extractedFields.toString() : "{}");
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
            
            // 2. Get activity PDFs
            List<byte[]> activityPdfs = getActivityPdfs(activities);
            
            // 3. Merge all PDFs
            return mergePdfs(summaryPdf, activityPdfs);
            
        } catch (Exception e) {
            throw new IOException("Failed to generate lesson plan: " + e.getMessage(), e);
        }
    }
    
    private List<byte[]> getActivityPdfs(List<Map<String, Object>> activities) {
        List<byte[]> pdfs = new ArrayList<>();
        
        for (Map<String, Object> activityMap : activities) {
            try {
                // Extract activity ID
                Object idObj = activityMap.get("id");
                if (idObj == null) continue;
                
                Long activityId = Long.parseLong(idObj.toString());
                
                // Get activity from database
                Activity activity = activityRepository.findById(activityId).orElse(null);
                if (activity == null) continue;
                
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
        PDFMergerUtility merger = new PDFMergerUtility();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try {
            // Add summary page first
            merger.addSource(new ByteArrayInputStream(summaryPdf));
            
            // Add all activity PDFs
            for (byte[] activityPdf : activityPdfs) {
                if (activityPdf != null && activityPdf.length > 0) {
                    merger.addSource(new ByteArrayInputStream(activityPdf));
                }
            }
            
            merger.setDestinationStream(outputStream);
            merger.mergeDocuments(null);
            
            return outputStream.toByteArray();
            
        } finally {
            outputStream.close();
        }
    }
    
    private byte[] generateSummaryPage(List<Map<String, Object>> activities,
                                      Map<String, Object> searchCriteria,
                                      List<Map<String, Object>> breaks,
                                      Integer totalDuration) throws IOException {
        /**
         * Generate a summary/cover page using PDFBox matching Flask's ReportLab output
         */
        
        PDDocument document = new PDDocument();
        
        try {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            
            try {
                float margin = 50;
                float yPosition = page.getMediaBox().getHeight() - margin;
                float lineHeight = 15;
                
                // Title
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Lesson Plan Summary");
                contentStream.endText();
                
                yPosition -= 40;
                
                // Search Criteria section
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Search Criteria:");
                contentStream.endText();
                
                yPosition -= 20;
                
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                if (searchCriteria != null && !searchCriteria.isEmpty()) {
                    for (Map.Entry<String, Object> entry : searchCriteria.entrySet()) {
                        if (entry.getValue() != null && !entry.getValue().toString().isEmpty()) {
                            String key = entry.getKey().replace("_", " ");
                            key = key.substring(0, 1).toUpperCase() + key.substring(1);
                            String text = key + ": " + entry.getValue().toString();
                            
                            contentStream.beginText();
                            contentStream.newLineAtOffset(margin + 10, yPosition);
                            contentStream.showText(text);
                            contentStream.endText();
                            
                            yPosition -= lineHeight;
                        }
                    }
                }
                
                yPosition -= 20;
                
                // Activities section
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Activities:");
                contentStream.endText();
                
                yPosition -= 20;
                
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                int activityNum = 1;
                for (Map<String, Object> activity : activities) {
                    String name = activity.getOrDefault("name", "N/A").toString();
                    Object durationObj = activity.get("duration_min_minutes");
                    String duration = durationObj != null ? durationObj.toString() + " min" : "N/A";
                    String format = activity.getOrDefault("format", "N/A").toString();
                    String bloomLevel = activity.getOrDefault("bloom_level", "N/A").toString();
                    
                    String text = String.format("%d. %s (%s, %s, %s)", 
                        activityNum++, name, duration, format, bloomLevel);
                    
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin + 10, yPosition);
                    contentStream.showText(text);
                    contentStream.endText();
                    
                    yPosition -= lineHeight;
                    
                    // Check if we need a new page
                    if (yPosition < margin + 100) {
                        break; // Stop adding activities if running out of space
                    }
                }
                
                // Breaks section
                if (breaks != null && !breaks.isEmpty()) {
                    yPosition -= 20;
                    
                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                    contentStream.newLineAtOffset(margin, yPosition);
                    contentStream.showText("Breaks:");
                    contentStream.endText();
                    
                    yPosition -= 20;
                    
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    for (Map<String, Object> breakItem : breaks) {
                        Object durationObj = breakItem.get("duration");
                        String duration = durationObj != null ? durationObj.toString() + " min" : "N/A";
                        String description = breakItem.getOrDefault("description", "Break").toString();
                        
                        String text = duration + " - " + description;
                        
                        contentStream.beginText();
                        contentStream.newLineAtOffset(margin + 10, yPosition);
                        contentStream.showText(text);
                        contentStream.endText();
                        
                        yPosition -= lineHeight;
                    }
                }
                
                // Total duration
                yPosition -= 20;
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Total Duration: " + (totalDuration != null ? totalDuration : 0) + " minutes");
                contentStream.endText();
                
                // Generated timestamp
                yPosition -= 30;
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                contentStream.newLineAtOffset(margin, yPosition);
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
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
}
