package com.learnhub.documentmanagement.controller;

import com.learnhub.activitymanagement.entity.enums.DocumentType;
import com.learnhub.documentmanagement.entity.PDFDocument;
import com.learnhub.documentmanagement.service.PDFService;
import com.learnhub.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Document management")
public class DocumentsController {

	private static final Logger logger = LoggerFactory.getLogger(DocumentsController.class);

	@Autowired
	private PDFService pdfService;

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
			response.put("fileSize", document.getFileSize());
			response.put("confidenceScore", document.getConfidenceScore());
			response.put("extractionQuality", document.getExtractionQuality());
			response.put("createdAt", document.getCreatedAt().toString());

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			logger.error("GET /api/documents/{}/info - Document not found: {}", documentId, e.getMessage());
			return ResponseEntity.status(404).body(ErrorResponse.of("Document not found: " + e.getMessage()));
		}
	}

	@GetMapping("/{documentId}/download")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Download document", description = "Download a stored document. SOURCE_PDF documents require admin rights.")
	@SecurityRequirement(name = "BearerAuth")
	public ResponseEntity<?> downloadDocument(@PathVariable UUID documentId, Authentication authentication)
			throws IOException {
		logger.info("GET /api/documents/{}/download - Download document called", documentId);
		try {
			PDFDocument document = pdfService.getPdfDocument(documentId);
			enforceDownloadAccess(document, authentication);

			byte[] pdfContent = pdfService.getPdfContent(documentId);
			String filename = sanitizeFilename(document.getFilename());

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.setContentDispositionFormData("attachment", filename);
			headers.setContentLength(pdfContent.length);

			return ResponseEntity.ok().headers(headers).body(pdfContent);
		} catch (AccessDeniedException e) {
			throw e;
		} catch (Exception e) {
			logger.error("GET /api/documents/{}/download - Document not found: {}", documentId, e.getMessage());
			return ResponseEntity.status(404).body(ErrorResponse.of("Document not found: " + e.getMessage()));
		}
	}

	private void enforceDownloadAccess(PDFDocument document, Authentication authentication) {
		if (document.getType() != DocumentType.SOURCE_PDF) {
			return;
		}

		boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
		if (!isAdmin) {
			throw new AccessDeniedException("Admin rights required to download source PDFs");
		}
	}

	private String sanitizeFilename(String name) {
		if (name == null || name.isBlank()) {
			return "document.pdf";
		}
		String sanitized = name.replaceAll("[^a-zA-Z0-9._\\- ]", "_").trim();
		return sanitized.isEmpty() ? "document.pdf" : sanitized;
	}
}
