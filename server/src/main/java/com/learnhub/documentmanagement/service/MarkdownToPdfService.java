package com.learnhub.documentmanagement.service;

import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.utils.PdfMerger;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
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

	private final MarkdownToHtmlService markdownToHtmlService;

	public MarkdownToPdfService(MarkdownToHtmlService markdownToHtmlService) {
		this.markdownToHtmlService = markdownToHtmlService;
	}

	/**
	 * Render markdown content to PDF bytes (default landscape).
	 */
	public byte[] renderMarkdownToPdf(String markdown) {
		return renderMarkdownToPdf(markdown, true);
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
		String html = markdownToHtmlService.renderMarkdownToHtml(markdown, landscape);
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			HtmlConverter.convertToPdf(html, baos);
			return baos.toByteArray();
		} catch (Exception e) {
			logger.error("Failed to render markdown PDF: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to render markdown PDF: " + e.getMessage(), e);
		}
	}

	/**
	 * Merge multiple PDF byte arrays into a single PDF document.
	 */
	public byte[] mergePdfs(List<byte[]> pdfParts) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PdfDocument mergedDoc = new PdfDocument(new PdfWriter(baos))) {
			PdfMerger merger = new PdfMerger(mergedDoc);

			for (byte[] pdfBytes : pdfParts) {
				try (PdfDocument srcDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfBytes)))) {
					merger.merge(srcDoc, 1, srcDoc.getNumberOfPages());
				}
			}

			mergedDoc.close();
			return baos.toByteArray();
		} catch (Exception e) {
			logger.error("Failed to merge PDFs: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to merge PDFs: " + e.getMessage(), e);
		}
	}
}
