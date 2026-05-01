package com.learnhub.documentmanagement.service;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Converts Markdown content to DOCX via Adobe PDF Services.
 *
 * Pipeline: Markdown → PDF ({@link MarkdownToPdfService}, which already
 * renders the complete LEARN-Hub layout including running headers, footers, and
 * page numbers via iText) → DOCX ({@link AdobePdfToDocxService}).
 *
 * Adobe preserves the PDF page layout — including the header/footer regions —
 * as native DOCX header/footer sections, so no post-processing is needed.
 */
@Service
public class MarkdownToDocxService {

	private static final Logger logger = LoggerFactory.getLogger(MarkdownToDocxService.class);

	private final MarkdownToPdfService markdownToPdfService;
	private final AdobePdfToDocxService adobePdfToDocxService;

	public MarkdownToDocxService(MarkdownToPdfService markdownToPdfService,
			AdobePdfToDocxService adobePdfToDocxService) {
		this.markdownToPdfService = markdownToPdfService;
		this.adobePdfToDocxService = adobePdfToDocxService;
	}

	public boolean isAvailable() {
		return adobePdfToDocxService.isConfigured();
	}

	public byte[] renderMarkdownToDocx(String markdown) {
		return renderMarkdownToDocx(markdown, true, "");
	}

	public byte[] renderMarkdownToDocx(String markdown, boolean landscape) {
		return renderMarkdownToDocx(markdown, landscape, "");
	}

	public byte[] renderMarkdownToDocx(String markdown, boolean landscape, String activityName) {
		return renderMarkdownToDocx(markdown, landscape, activityName, false);
	}

	public byte[] renderMarkdownToDocx(String markdown, boolean landscape, String activityName, boolean exerciseSheet) {
		try {
			byte[] pdfBytes = markdownToPdfService.renderMarkdownToPdf(markdown, landscape, activityName, exerciseSheet);
			return adobePdfToDocxService.convertPdfToDocx(pdfBytes);
		} catch (Exception e) {
			logger.error("Failed to render markdown to DOCX: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to render markdown to DOCX: " + e.getMessage(), e);
		}
	}

	public byte[] renderHtmlToDocx(String htmlBody, boolean landscape, String activityName) {
		try {
			byte[] pdfBytes = markdownToPdfService.renderHtmlToPdf(htmlBody, landscape, activityName);
			return adobePdfToDocxService.convertPdfToDocx(pdfBytes);
		} catch (Exception e) {
			logger.error("Failed to render HTML to DOCX: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to render HTML to DOCX: " + e.getMessage(), e);
		}
	}

	/**
	 * Render multiple markdown sections as a single DOCX. All sections are
	 * rendered as PDFs, merged into one with iText, then converted in a single
	 * Adobe API call — this avoids image-relationship corruption that occurs when
	 * merging DOCX files at the XML level.
	 *
	 * @param exerciseSheets
	 *            per-section flag; {@code true} activates the exercise-sheet border
	 *            layout (outer border, Name/Datum fields). Must be the same size as
	 *            {@code markdowns}.
	 */
	public byte[] renderMergedDocx(List<String> markdowns, List<Boolean> landscapes, List<Boolean> exerciseSheets,
			String activityName) {
		if (markdowns.size() != landscapes.size() || markdowns.size() != exerciseSheets.size()) {
			throw new IllegalArgumentException("markdowns, landscapes, and exerciseSheets lists must have the same size");
		}
		try {
			List<byte[]> pdfParts = new ArrayList<>();
			for (int i = 0; i < markdowns.size(); i++) {
				pdfParts.add(markdownToPdfService.renderMarkdownToPdf(markdowns.get(i), landscapes.get(i),
						activityName, exerciseSheets.get(i)));
			}
			byte[] mergedPdf = markdownToPdfService.mergePdfs(pdfParts);
			return adobePdfToDocxService.convertPdfToDocx(mergedPdf);
		} catch (Exception e) {
			logger.error("Failed to render merged markdown to DOCX: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to render merged markdown to DOCX: " + e.getMessage(), e);
		}
	}
}
