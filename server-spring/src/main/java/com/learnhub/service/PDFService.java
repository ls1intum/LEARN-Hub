package com.learnhub.service;

import com.learnhub.dto.response.LessonPlanInfoResponse;
import com.learnhub.model.PDFDocument;
import com.learnhub.repository.PDFDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PDFService {

    @Autowired
    private PDFDocumentRepository pdfDocumentRepository;

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
        // This is a simplified implementation
        // In the real Flask implementation, this merges PDFs with cover page and break pages
        // For now, we'll just return a placeholder or the first PDF
        
        if (activities.isEmpty()) {
            throw new RuntimeException("No activities provided");
        }
        
        // Get the first activity's PDF as a placeholder
        // In a real implementation, you would merge all PDFs with cover pages and breaks
        Map<String, Object> firstActivity = activities.get(0);
        Object docIdObj = firstActivity.get("document_id");
        
        if (docIdObj != null) {
            Long documentId = Long.parseLong(docIdObj.toString());
            return getPdfContent(documentId);
        }
        
        throw new RuntimeException("No PDF available for lesson plan generation");
    }
}
