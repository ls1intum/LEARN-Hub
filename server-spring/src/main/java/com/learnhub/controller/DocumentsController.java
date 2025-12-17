package com.learnhub.controller;

import com.learnhub.dto.response.ErrorResponse;
import com.learnhub.model.PDFDocument;
import com.learnhub.service.PDFService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Document management")
public class DocumentsController {

    @Autowired
    private PDFService pdfService;

    @PostMapping("/upload_pdf")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Upload PDF", description = "Upload and process PDF document for activity creation")
    public ResponseEntity<?> uploadPdf(
            @RequestParam("pdf_file") MultipartFile pdfFile) {
        try {
            if (pdfFile.isEmpty()) {
                return ResponseEntity.badRequest().body(ErrorResponse.of("No PDF file provided"));
            }

            if (!pdfFile.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
                return ResponseEntity.badRequest().body(ErrorResponse.of("File must be a PDF"));
            }

            byte[] pdfContent = pdfFile.getBytes();
            if (pdfContent.length == 0) {
                return ResponseEntity.badRequest().body(ErrorResponse.of("PDF file is empty"));
            }

            Long documentId = pdfService.storePdf(pdfContent, pdfFile.getOriginalFilename());

            Map<String, Object> response = new HashMap<>();
            response.put("document_id", documentId);
            response.put("filename", pdfFile.getOriginalFilename());
            response.put("file_size", pdfContent.length);
            response.put("message", "PDF uploaded successfully");

            return ResponseEntity.status(201).body(ResponseEntity.ok(response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ErrorResponse.of("Failed to upload PDF: " + e.getMessage()));
        }
    }

    @GetMapping("/{documentId}")
    @Operation(summary = "Get document", description = "Retrieve PDF file content by document ID")
    public ResponseEntity<?> getDocument(@PathVariable Long documentId) {
        try {
            PDFDocument document = pdfService.getPdfDocument(documentId);
            byte[] pdfContent = pdfService.getPdfContent(documentId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", document.getFilename());
            headers.setContentLength(pdfContent.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfContent);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(ErrorResponse.of("Document not found: " + e.getMessage()));
        }
    }

    @GetMapping("/{documentId}/info")
    @Operation(summary = "Get document info", description = "Get PDF document metadata")
    public ResponseEntity<?> getDocumentInfo(@PathVariable Long documentId) {
        try {
            PDFDocument document = pdfService.getPdfDocument(documentId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", document.getId());
            response.put("filename", document.getFilename());
            response.put("file_size", document.getFileSize());
            response.put("confidence_score", document.getConfidenceScore());
            response.put("extraction_quality", document.getExtractionQuality());
            response.put("created_at", document.getCreatedAt().toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(ErrorResponse.of("Document not found: " + e.getMessage()));
        }
    }
}
