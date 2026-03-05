package com.learnhub.documentmanagement.controller;

import com.learnhub.documentmanagement.entity.PDFDocument;
import com.learnhub.documentmanagement.service.PDFService;
import com.learnhub.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Document management")
public class DocumentsController {

	private static final Logger logger = LoggerFactory.getLogger(DocumentsController.class);

	private final PDFService pdfService;

	public DocumentsController(PDFService pdfService) {
		this.pdfService = pdfService;
	}

	@PostMapping("/upload_pdf")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Upload PDF", description = "Upload and process PDF document for activity creation")
	public ResponseEntity<?> uploadPdf(@RequestParam("pdf_file") MultipartFile pdfFile) {
		logger.info("POST /api/documents/upload_pdf - Upload PDF called with filename={}",
				pdfFile.getOriginalFilename());
		try {
			if (pdfFile.isEmpty()) {
				logger.error("POST /api/documents/upload_pdf - No PDF file provided");
				return ResponseEntity.badRequest().body(ErrorResponse.of("No PDF file provided"));
			}

			String filename = pdfFile.getOriginalFilename();
			if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
				logger.error("POST /api/documents/upload_pdf - File is not a PDF: {}", filename);
				return ResponseEntity.badRequest().body(ErrorResponse.of("File must be a PDF"));
			}

			byte[] pdfContent = pdfFile.getBytes();
			if (pdfContent.length == 0) {
				logger.error("POST /api/documents/upload_pdf - PDF file is empty");
				return ResponseEntity.badRequest().body(ErrorResponse.of("PDF file is empty"));
			}

			UUID documentId = pdfService.storePdf(pdfContent, pdfFile.getOriginalFilename());

			logger.info("POST /api/documents/upload_pdf - PDF uploaded successfully with documentId={}, size={} bytes",
					documentId, pdfContent.length);
			Map<String, Object> response = new HashMap<>();
			response.put("document_id", documentId);
			response.put("filename", pdfFile.getOriginalFilename());
			response.put("file_size", pdfContent.length);

			return ResponseEntity.status(201).body(response);
		} catch (Exception e) {
			logger.error("POST /api/documents/upload_pdf - Failed to upload PDF: {}", e.getMessage());
			return ResponseEntity.status(500).body(ErrorResponse.of("Failed to upload PDF: " + e.getMessage()));
		}
	}

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

	@PostMapping("/{documentId}/process")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Process PDF document", description = "Extract activity data from PDF document using LLM")
	public ResponseEntity<?> processPdf(@PathVariable UUID documentId) {
		logger.info("POST /api/documents/{}/process - Process PDF called", documentId);
		try {
			// Get PDF content
			byte[] pdfContent = pdfService.getPdfContent(documentId);
			if (pdfContent == null) {
				logger.error("POST /api/documents/{}/process - PDF document not found", documentId);
				return ResponseEntity.status(404).body(ErrorResponse.of("PDF document not found"));
			}

			// Extract activity data from PDF using LLM
			Map<String, Object> extractionResult = pdfService.extractActivityFromPdf(pdfContent, documentId);

			if (extractionResult.containsKey("error")) {
				String errorMsg = (String) extractionResult.get("error");
				logger.error("POST /api/documents/{}/process - PDF extraction failed: {}", documentId, errorMsg);
				return ResponseEntity.status(400).body(ErrorResponse.of("Failed to process PDF: " + errorMsg));
			}

			// Update PDF document with extraction results
			Map<String, Object> data = (Map<String, Object>) extractionResult.get("data");
			Double confidence = (Double) extractionResult.get("confidence");
			String confidenceScore = confidence != null ? String.format("%.3f", confidence) : null;
			String extractionQuality = (String) extractionResult.get("extraction_quality");

			pdfService.updatePdfExtractionResults(documentId, data, confidenceScore, extractionQuality);

			logger.info("POST /api/documents/{}/process - PDF processed successfully, confidence={}, quality={}",
					documentId, confidenceScore, extractionQuality);

			// Prepare response matching Flask format
			Map<String, Object> response = new HashMap<>();
			response.put("document_id", documentId);
			response.put("extracted_data", data);
			response.put("confidence", confidence);
			response.put("text_length", extractionResult.get("text_length"));
			response.put("extraction_quality", extractionQuality);

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			logger.error("POST /api/documents/{}/process - Failed to process PDF: {}", documentId, e.getMessage());
			return ResponseEntity.status(500).body(ErrorResponse.of("Failed to process PDF: " + e.getMessage()));
		}
	}
}
