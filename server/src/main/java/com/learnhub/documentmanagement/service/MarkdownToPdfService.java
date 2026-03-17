package com.learnhub.documentmanagement.service;

import com.itextpdf.html2pdf.HtmlConverter;
import java.io.ByteArrayOutputStream;
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
	 * Render markdown content to PDF bytes.
	 */
	public byte[] renderMarkdownToPdf(String markdown) {
		String html = markdownToHtmlService.renderMarkdownToHtml(markdown);
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			HtmlConverter.convertToPdf(html, baos);
			return baos.toByteArray();
		} catch (Exception e) {
			logger.error("Failed to render markdown PDF: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to render markdown PDF: " + e.getMessage(), e);
		}
	}
}
