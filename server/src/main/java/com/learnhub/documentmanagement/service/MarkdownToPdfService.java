package com.learnhub.documentmanagement.service;

import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.resolver.font.DefaultFontProvider;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.utils.PdfMerger;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
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
	private static final List<String> WORKSHEET_FONT_PATHS = List.of(
			"/System/Library/Fonts/Supplemental/Comic Sans MS.ttf",
			"/System/Library/Fonts/Supplemental/Comic Sans MS Bold.ttf",
			"/Library/Fonts/Comic Sans MS.ttf",
			"/Library/Fonts/Comic Sans MS Bold.ttf",
			"C:/Windows/Fonts/comic.ttf",
			"C:/Windows/Fonts/comicbd.ttf");

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
			HtmlConverter.convertToPdf(html, baos, createConverterProperties());
			return applyDocumentTitle(baos.toByteArray(), documentTitle);
		} catch (Exception e) {
			logger.error("Failed to render markdown PDF: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to render markdown PDF: " + e.getMessage(), e);
		}
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
			logger.warn("Worksheet PDF font '{}' was not registered from the known font paths. Falling back to other system fonts.",
					WORKSHEET_FONT_FAMILY);
		}

		return new ConverterProperties().setFontProvider(fontProvider);
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
