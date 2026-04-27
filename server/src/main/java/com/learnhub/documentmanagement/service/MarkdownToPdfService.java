package com.learnhub.documentmanagement.service;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.html2pdf.resolver.font.DefaultFontProvider;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.utils.PdfMerger;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for rendering Markdown content into a styled PDF using iText html2pdf
 * and {@link MarkdownToHtmlService} for the Markdown-to-HTML conversion step.
 */
@Service
public class MarkdownToPdfService {

	private static final Logger logger = LoggerFactory.getLogger(MarkdownToPdfService.class);
	private static final String WORKSHEET_FONT_FAMILY = "Comic Sans MS";
	private static final int MAX_CMAP_RETRY_CHARACTERS = 256;
	private static final List<String> WORKSHEET_FONT_PATHS = List.of(
			"/usr/share/fonts/truetype/comic-sans/ComicSansMS.ttf",
			"/usr/share/fonts/truetype/comic-sans/ComicSansMSBold.ttf",
			"/System/Library/Fonts/Supplemental/Comic Sans MS.ttf",
			"/System/Library/Fonts/Supplemental/Comic Sans MS Bold.ttf", "/Library/Fonts/Comic Sans MS.ttf",
			"/Library/Fonts/Comic Sans MS Bold.ttf", "C:/Windows/Fonts/comic.ttf", "C:/Windows/Fonts/comicbd.ttf");

	private final MarkdownToHtmlService markdownToHtmlService;

	public MarkdownToPdfService(MarkdownToHtmlService markdownToHtmlService) {
		this.markdownToHtmlService = markdownToHtmlService;
	}

	/**
	 * Render markdown content to PDF bytes (default landscape).
	 */
	public byte[] renderMarkdownToPdf(String markdown) {
		return renderMarkdownToPdf(markdown, true, "");
	}

	/**
	 * Render markdown content to PDF bytes with specified orientation.
	 *
	 * @param markdown
	 *            the markdown content
	 * @param landscape
	 *            true for landscape, false for portrait
	 */
	public byte[] renderMarkdownToPdf(String markdown, boolean landscape) {
		return renderMarkdownToPdf(markdown, landscape, "");
	}

	/**
	 * Render markdown content to PDF bytes with specified orientation and activity
	 * name in header.
	 *
	 * @param markdown
	 *            the markdown content
	 * @param landscape
	 *            true for landscape, false for portrait
	 * @param activityName
	 *            the activity name shown in the page header
	 */
	public byte[] renderMarkdownToPdf(String markdown, boolean landscape, String activityName) {
		String html = markdownToHtmlService.renderMarkdownToHtml(markdown, landscape, activityName);
		return renderHtmlDocumentToPdf(html, activityName);
	}

	/**
	 * Render an existing HTML body to PDF bytes with the standard LEARN-Hub PDF
	 * layout.
	 */
	public byte[] renderHtmlToPdf(String htmlBody, boolean landscape, String activityName) {
		String html = markdownToHtmlService.renderHtmlBodyToHtmlDocument(htmlBody, landscape, activityName);
		return renderHtmlDocumentToPdf(html, activityName);
	}

	private byte[] renderHtmlDocumentToPdf(String html) {
		return renderHtmlDocumentToPdf(html, null);
	}

	private byte[] renderHtmlDocumentToPdf(String html, String documentTitle) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			convertHtmlDocumentToPdfBytes(html, baos, createConverterProperties());
			return applyDocumentTitle(baos.toByteArray(), documentTitle);
		} catch (Exception e) {
			if (isCmapFailure(e)) {
				return retryRenderWithoutProblematicCharacter(html, documentTitle, e);
			}
			logger.error("Failed to render markdown PDF: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to render markdown PDF: " + e.getMessage(), e);
		}
	}

	void convertHtmlDocumentToPdfBytes(String html, ByteArrayOutputStream outputStream,
			ConverterProperties converterProperties) {
		HtmlConverter.convertToPdf(html, outputStream, converterProperties);
	}

	ConverterProperties createConverterProperties() {
		// Use iText's bundled/shipped fonts for Unicode fallback instead of
		// addSystemFonts(), which on macOS/Linux includes Apple-specific fonts with
		// incomplete cmap tables that cause a NullPointerException during rendering.
		DefaultFontProvider fontProvider = new DefaultFontProvider(false, false, true, WORKSHEET_FONT_FAMILY);

		boolean worksheetFontRegistered = false;
		for (String fontPath : WORKSHEET_FONT_PATHS) {
			if (!Files.exists(Path.of(fontPath))) {
				continue;
			}
			if (fontProvider.addFont(fontPath)) {
				worksheetFontRegistered = true;
			}
		}

		if (!worksheetFontRegistered) {
			logger.warn(
					"Worksheet PDF font '{}' was not registered from the known font paths. Falling back to other system fonts.",
					WORKSHEET_FONT_FAMILY);
		}

		return new ConverterProperties().setFontProvider(fontProvider);
	}

	private byte[] retryRenderWithoutProblematicCharacter(String html, String documentTitle, Exception originalException) {
		Set<Integer> candidateCodePoints = findProblematicCharacterCandidates(html);
		for (int codePoint : candidateCodePoints) {
			String sanitizedHtml = removeCodePoint(html, codePoint);
			if (sanitizedHtml.equals(html)) {
				continue;
			}

			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				convertHtmlDocumentToPdfBytes(sanitizedHtml, baos, createConverterProperties());
				logger.warn(
						"Rendered markdown PDF after removing character {} (U+{}) because iText failed with a null cmap table.",
						describeCodePoint(codePoint), formatCodePoint(codePoint));
				return applyDocumentTitle(baos.toByteArray(), documentTitle);
			} catch (Exception retryException) {
				logger.debug("Retry after removing character {} (U+{}) still failed: {}", describeCodePoint(codePoint),
						formatCodePoint(codePoint), retryException.getMessage());
			}
		}

		logger.error("Failed to render markdown PDF: {}", originalException.getMessage(), originalException);
		throw new RuntimeException("Failed to render markdown PDF: " + originalException.getMessage(), originalException);
	}

	private Set<Integer> findProblematicCharacterCandidates(String html) {
		Set<Integer> candidates = new LinkedHashSet<>();
		html.codePoints().filter(this::shouldRetryWithoutCodePoint).limit(MAX_CMAP_RETRY_CHARACTERS).forEach(candidates::add);
		return candidates;
	}

	private boolean shouldRetryWithoutCodePoint(int codePoint) {
		if (Character.isWhitespace(codePoint)) {
			return false;
		}
		if (codePoint >= 0x20 && codePoint <= 0x7E) {
			return false;
		}
		return !Character.isISOControl(codePoint);
	}

	private String removeCodePoint(String text, int codePoint) {
		return text.codePoints().filter(current -> current != codePoint)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
	}

	private boolean isCmapFailure(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			String message = current.getMessage();
			if (message != null && message.toLowerCase(Locale.ROOT).contains("cmap")) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private String describeCodePoint(int codePoint) {
		if (Character.isSupplementaryCodePoint(codePoint)) {
			return new String(Character.toChars(codePoint));
		}
		return Character.toString((char) codePoint);
	}

	private String formatCodePoint(int codePoint) {
		return String.format(Locale.ROOT, "%04X", codePoint);
	}

	public byte[] applyDocumentTitle(byte[] pdfBytes, String documentTitle) {
		String normalizedTitle = normalizeDocumentTitle(documentTitle);
		if (normalizedTitle == null || pdfBytes == null || pdfBytes.length == 0) {
			return pdfBytes;
		}

		try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfBytes);
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				PdfDocument pdfDocument = new PdfDocument(new PdfReader(inputStream), new PdfWriter(outputStream))) {
			pdfDocument.getDocumentInfo().setTitle(normalizedTitle);
			pdfDocument.close();
			return outputStream.toByteArray();
		} catch (Exception e) {
			logger.warn("Failed to set PDF document title '{}': {}", normalizedTitle, e.getMessage());
			return pdfBytes;
		}
	}

	/**
	 * Merge multiple PDF byte arrays into a single PDF document.
	 */
	public byte[] mergePdfs(List<byte[]> pdfParts) {
		return mergePdfs(pdfParts, null);
	}

	public byte[] mergePdfs(List<byte[]> pdfParts, String documentTitle) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PdfDocument mergedDoc = new PdfDocument(new PdfWriter(baos))) {
			PdfMerger merger = new PdfMerger(mergedDoc);

			for (byte[] pdfBytes : pdfParts) {
				try (PdfDocument srcDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfBytes)))) {
					merger.merge(srcDoc, 1, srcDoc.getNumberOfPages());
				}
			}

			mergedDoc.close();
			return applyDocumentTitle(baos.toByteArray(), documentTitle);
		} catch (Exception e) {
			logger.error("Failed to merge PDFs: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to merge PDFs: " + e.getMessage(), e);
		}
	}

	private String normalizeDocumentTitle(String title) {
		if (title == null) {
			return null;
		}

		String normalized = title.trim();
		return normalized.isEmpty() ? null : normalized;
	}
}
