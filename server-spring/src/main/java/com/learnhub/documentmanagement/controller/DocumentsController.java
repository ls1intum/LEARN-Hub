package com.learnhub.documentmanagement.controller;

import com.learnhub.documentmanagement.entity.PDFDocument;
import com.learnhub.documentmanagement.service.PDFService;
import com.learnhub.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
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

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Document management")
public class DocumentsController {

	private static final Logger logger = LoggerFactory.getLogger(DocumentsController.class);

	@Autowired
	private PDFService pdfService;

	@GetMapping("/{documentId}")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Get document", description = "Retrieve PDF file content by document ID")
	public ResponseEntity<?> getDocument(@PathVariable UUID documentId) {
		logger.info("GET /api/documents/{} - Get document called", documentId);
		try {
			PDFDocument document = pdfService.getPdfDocument(documentId);
			byte[] pdfContent = pdfService.getPdfContent(documentId);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.setContentDispositionFormData("inline", document.getFilename());
			headers.setContentLength(pdfContent.length);

			return ResponseEntity.ok().headers(headers).body(pdfContent);
		} catch (Exception e) {
			logger.error("GET /api/documents/{} - Document not found: {}", documentId, e.getMessage());
			return ResponseEntity.status(404).body(ErrorResponse.of("Document not found: " + e.getMessage()));
		}
	}

	@GetMapping("/{documentId}/info")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Get document info", description = "Get PDF document metadata")
	public ResponseEntity<?> getDocumentInfo(@PathVariable UUID documentId) {
		logger.info("GET /api/documents/{}/info - Get document info called", documentId);
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
			logger.error("GET /api/documents/{}/info - Document not found: {}", documentId, e.getMessage());
			return ResponseEntity.status(404).body(ErrorResponse.of("Document not found: " + e.getMessage()));
		}
	}
}
