package com.learnhub.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhub.documentmanagement.dto.response.TestMarkdownResponse;
import com.learnhub.documentmanagement.service.LLMService;
import com.learnhub.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Development-only controller for testing LLM markdown generation without any
 * persistence. Active only when the {@code dev} Spring profile is set.
 *
 * <p>
 * The uploaded PDF is never stored — text is extracted on-the-fly and discarded
 * after the request completes.
 */
@Profile("dev")
@RestController
@RequestMapping("/api/dev")
@Tag(name = "Dev", description = "Development and testing utilities (dev profile only)")
public class DevController {

	private static final Logger logger = LoggerFactory.getLogger(DevController.class);
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Value("${llm.visual.max-pages:5}")
	private int visionMaxPages;

	@Value("${llm.visual.dpi:72}")
	private int visionDpi;

	@Autowired
	private LLMService llmService;

	/**
	 * Test a single markdown generation without any persistence.
	 *
	 * <p>
	 * Accepts a raw PDF upload, extracts its text (and optionally renders page
	 * images for vision models), calls the LLM, and returns the generated markdown
	 * directly. Nothing is saved to the database or file cache.
	 *
	 * @param file
	 *            the PDF file to process
	 * @param type
	 *            markdown type: uebung (default), deckblatt, hintergrundwissen,
	 *            artikulationsschema
	 * @param metadataJson
	 *            optional JSON object with activity metadata
	 */
	@PostMapping(value = "/test-markdown", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('ADMIN')")
	@SecurityRequirement(name = "BearerAuth")
	@Operation(summary = "Test markdown generation (dev only)", description = "Upload a PDF and generate markdown without persisting anything. "
			+ "Available in dev profile only.")
	public ResponseEntity<?> testMarkdown(@RequestPart("file") MultipartFile file,
			@RequestParam(value = "type", defaultValue = "uebung") String type,
			@RequestParam(value = "metadata", required = false) String metadataJson) {

		logger.info("POST /api/dev/test-markdown - type={}, file={} ({} bytes)", type, file.getOriginalFilename(),
				file.getSize());
		try {
			byte[] pdfBytes = file.getBytes();
			String pdfText = extractPdfText(pdfBytes);
			Map<String, Object> metadata = parseMetadata(metadataJson);

			TestMarkdownResponse response = new TestMarkdownResponse();

			switch (type.toLowerCase()) {
				case "uebung" -> {
					List<byte[]> images = llmService.isVisionEnabled() ? renderPdfImages(pdfBytes) : null;
					Map<String, String> result = llmService.generateUebungAndLoesung(pdfText, metadata, images);
					response.setUebungMarkdown(result.get("uebung"));
					response.setUebungLoesungMarkdown(result.get("uebung_loesung"));
				}
				case "deckblatt" -> response.setDeckblattMarkdown(llmService.generateDeckblatt(pdfText, metadata));
				case "hintergrundwissen" ->
					response.setHintergrundwissenMarkdown(llmService.generateHintergrundwissen(pdfText, metadata));
				case "artikulationsschema" ->
					response.setArtikulationsschemaMarkdown(llmService.generateArtikulationsschema(pdfText, metadata));
				default -> {
					return ResponseEntity.badRequest().body(ErrorResponse.of("Unknown type: " + type
							+ ". Valid values: uebung, deckblatt, hintergrundwissen, artikulationsschema"));
				}
			}

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			logger.error("POST /api/dev/test-markdown - Failed: {}", e.getMessage(), e);
			return ResponseEntity.status(500).body(ErrorResponse.of("Markdown generation failed: " + e.getMessage()));
		}
	}

	private String extractPdfText(byte[] pdfBytes) throws IOException {
		try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
			return new PDFTextStripper().getText(doc);
		}
	}

	private List<byte[]> renderPdfImages(byte[] pdfBytes) throws IOException {
		try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
			int pageCount = Math.min(doc.getNumberOfPages(), visionMaxPages);
			PDFRenderer renderer = new PDFRenderer(doc);
			List<byte[]> images = new ArrayList<>();
			for (int i = 0; i < pageCount; i++) {
				BufferedImage image = renderer.renderImageWithDPI(i, visionDpi);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(image, "jpg", baos);
				images.add(baos.toByteArray());
			}
			return images;
		}
	}

	private Map<String, Object> parseMetadata(String metadataJson) throws IOException {
		if (metadataJson == null || metadataJson.isBlank()) {
			return Map.of();
		}
		return OBJECT_MAPPER.readValue(metadataJson, new TypeReference<>() {
		});
	}
}
