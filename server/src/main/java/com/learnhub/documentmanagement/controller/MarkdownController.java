package com.learnhub.documentmanagement.controller;

import com.learnhub.activitymanagement.entity.ActivityMarkdown;
import com.learnhub.activitymanagement.repository.ActivityMarkdownRepository;
import com.learnhub.documentmanagement.service.MarkdownToDocxService;
import com.learnhub.documentmanagement.service.MarkdownToPdfService;
import com.learnhub.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/markdowns")
@Tag(name = "Markdowns", description = "Markdown rendering and export endpoints")
public class MarkdownController {

	private static final Logger logger = LoggerFactory.getLogger(MarkdownController.class);

	@Autowired
	private ActivityMarkdownRepository markdownRepository;

	@Autowired
	private MarkdownToPdfService markdownToPdfService;

	@Autowired
	private MarkdownToDocxService markdownToDocxService;

	@GetMapping("/{markdownId}/pdf")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Get markdown as PDF", description = "Render a stored markdown entry as a PDF document")
	public ResponseEntity<?> getMarkdownPdf(@PathVariable UUID markdownId) {
		logger.info("GET /api/markdowns/{}/pdf - Render markdown as PDF", markdownId);
		try {
			ActivityMarkdown markdown = markdownRepository.findById(markdownId).orElse(null);
			if (markdown == null) {
				return ResponseEntity.status(404).body(ErrorResponse.of("Markdown not found"));
			}
			String content = markdown.getContent();
			if (content == null || content.trim().isEmpty()) {
				return ResponseEntity.status(404).body(ErrorResponse.of("Markdown content is empty"));
			}

			byte[] pdfBytes = markdownToPdfService.renderMarkdownToPdf(content, markdown.isLandscape(),
					markdown.getActivity() != null ? markdown.getActivity().getName() : "");

			String downloadName = sanitizeFilename(markdown.getType().getValue()) + ".pdf";

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.setContentDispositionFormData("inline", downloadName);
			headers.setContentLength(pdfBytes.length);

			return ResponseEntity.ok().headers(headers).body(pdfBytes);
		} catch (Exception e) {
			logger.error("GET /api/markdowns/{}/pdf - Failed: {}", markdownId, e.getMessage());
			return ResponseEntity.status(500).body(ErrorResponse.of("Failed to render PDF: " + e.getMessage()));
		}
	}

	@GetMapping("/{markdownId}/docx")
	@PreAuthorize("permitAll()")
	@Operation(summary = "Get markdown as DOCX", description = "Render a stored markdown entry as a Word document")
	public ResponseEntity<?> getMarkdownDocx(@PathVariable UUID markdownId) {
		logger.info("GET /api/markdowns/{}/docx - Render markdown as DOCX", markdownId);
		try {
			ActivityMarkdown markdown = markdownRepository.findById(markdownId).orElse(null);
			if (markdown == null) {
				return ResponseEntity.status(404).body(ErrorResponse.of("Markdown not found"));
			}
			String content = markdown.getContent();
			if (content == null || content.trim().isEmpty()) {
				return ResponseEntity.status(404).body(ErrorResponse.of("Markdown content is empty"));
			}

			byte[] docxBytes = markdownToDocxService.renderMarkdownToDocx(content, markdown.isLandscape());

			String downloadName = sanitizeFilename(markdown.getType().getValue()) + ".docx";

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType
					.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
			headers.setContentDispositionFormData("attachment", downloadName);
			headers.setContentLength(docxBytes.length);

			return ResponseEntity.ok().headers(headers).body(docxBytes);
		} catch (Exception e) {
			logger.error("GET /api/markdowns/{}/docx - Failed: {}", markdownId, e.getMessage());
			return ResponseEntity.status(500).body(ErrorResponse.of("Failed to render DOCX: " + e.getMessage()));
		}
	}

	@PostMapping("/preview-pdf")
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Preview markdown as PDF", description = "Render raw markdown text to a preview PDF (admin only)")
	public ResponseEntity<?> previewMarkdownPdf(@RequestBody Map<String, String> request) {
		logger.info("POST /api/markdowns/preview-pdf called");
		try {
			String markdown = request.get("markdown");
			if (markdown == null || markdown.trim().isEmpty()) {
				return ResponseEntity.badRequest().body(ErrorResponse.of("markdown is required"));
			}

			// Preview uses landscape by default; orientation param is optional
			String orientation = request.get("orientation");
			boolean landscape = orientation == null || !"portrait".equalsIgnoreCase(orientation);
			byte[] pdfBytes = markdownToPdfService.renderMarkdownToPdf(markdown, landscape);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.setContentDispositionFormData("inline", "markdown_preview.pdf");
			headers.setContentLength(pdfBytes.length);

			return ResponseEntity.ok().headers(headers).body(pdfBytes);
		} catch (Exception e) {
			logger.error("POST /api/markdowns/preview-pdf - Failed: {}", e.getMessage());
			return ResponseEntity.status(500).body(ErrorResponse.of("Failed to render preview PDF: " + e.getMessage()));
		}
	}

	private String sanitizeFilename(String name) {
		if (name == null || name.isBlank()) {
			return "markdown";
		}
		String sanitized = name.replaceAll("[^a-zA-Z0-9._\\- ]", "_").trim();
		return sanitized.isEmpty() ? "markdown" : sanitized;
	}
}
