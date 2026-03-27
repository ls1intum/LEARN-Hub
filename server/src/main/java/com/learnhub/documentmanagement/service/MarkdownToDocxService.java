package com.learnhub.documentmanagement.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for rendering Markdown content to DOCX (Word) format. Generates
 * styled HTML via {@link MarkdownToHtmlService}, converts it to DOCX using
 * {@link LibreOfficeConversionService} (LibreOffice headless), then
 * post-processes the DOCX with {@link DocxPostProcessor} to add headers,
 * footers, and correct page orientation.
 */
@Service
public class MarkdownToDocxService {

	private static final Logger logger = LoggerFactory.getLogger(MarkdownToDocxService.class);

	private static final String FONT_FAMILY = "Helvetica, Arial, sans-serif";

	/**
	 * Matches the iText-specific running header/footer divs that should not appear
	 * as body content in the DOCX.
	 */
	private static final Pattern RUNNING_ELEMENT_PATTERN = Pattern.compile(
			"<div\\s+id=\"(?:page-header|footer-date|footer-center)\"[^>]*>.*?</div>\\s*",
			Pattern.DOTALL);

	/** Matches {@code <table}, {@code <th}, {@code <td}, {@code <thead} opening tags. */
	private static final Pattern TABLE_TAG_PATTERN = Pattern.compile(
			"<(table|th|td)(\\s|>)", Pattern.CASE_INSENSITIVE);

	/** Matches {@code <thead>} sections to find header rows. */
	private static final Pattern THEAD_PATTERN = Pattern.compile(
			"<thead>(.*?)</thead>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

	/** Matches {@code <th} tags inside thead for inline styling. */
	private static final Pattern TH_TAG_PATTERN = Pattern.compile(
			"<th(\\s|>)", Pattern.CASE_INSENSITIVE);

	/** Matches any opening HTML tag for font injection. */
	private static final Pattern HEADING_TAG_PATTERN = Pattern.compile(
			"<(h[1-6])(\\s|>)", Pattern.CASE_INSENSITIVE);

	private final MarkdownToHtmlService markdownToHtmlService;
	private final LibreOfficeConversionService libreOfficeConversionService;
	private final DocxPostProcessor docxPostProcessor;

	public MarkdownToDocxService(MarkdownToHtmlService markdownToHtmlService,
			LibreOfficeConversionService libreOfficeConversionService, DocxPostProcessor docxPostProcessor) {
		this.markdownToHtmlService = markdownToHtmlService;
		this.libreOfficeConversionService = libreOfficeConversionService;
		this.docxPostProcessor = docxPostProcessor;
	}

	public byte[] renderMarkdownToDocx(String markdown) {
		return renderMarkdownToDocx(markdown, true, "");
	}

	public byte[] renderMarkdownToDocx(String markdown, boolean landscape) {
		return renderMarkdownToDocx(markdown, landscape, "");
	}

	public byte[] renderMarkdownToDocx(String markdown, boolean landscape, String activityName) {
		try {
			String html = prepareHtmlForDocx(
					markdownToHtmlService.renderMarkdownToHtml(markdown, landscape, activityName));
			byte[] rawDocx = libreOfficeConversionService.convertHtmlToDocx(html.getBytes(StandardCharsets.UTF_8));
			return docxPostProcessor.process(rawDocx, landscape, activityName);
		} catch (Exception e) {
			logger.error("Failed to render markdown to DOCX: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to render markdown to DOCX: " + e.getMessage(), e);
		}
	}

	public byte[] renderHtmlToDocx(String htmlBody, boolean landscape, String activityName) {
		try {
			String html = prepareHtmlForDocx(
					markdownToHtmlService.renderHtmlBodyToHtmlDocument(htmlBody, landscape, activityName));
			byte[] rawDocx = libreOfficeConversionService.convertHtmlToDocx(html.getBytes(StandardCharsets.UTF_8));
			return docxPostProcessor.process(rawDocx, landscape, activityName);
		} catch (Exception e) {
			logger.error("Failed to render HTML to DOCX: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to render HTML to DOCX: " + e.getMessage(), e);
		}
	}

	public byte[] renderMergedDocx(List<String> markdowns, List<Boolean> landscapes, String activityName) {
		if (markdowns.size() != landscapes.size()) {
			throw new IllegalArgumentException("markdowns and landscapes lists must have the same size");
		}
		try {
			StringBuilder combinedBody = new StringBuilder();
			for (int i = 0; i < markdowns.size(); i++) {
				if (i > 0) {
					combinedBody.append("<div style=\"page-break-before: always;\"></div>\n");
				}
				String sectionHtml = markdownToHtmlService.renderMarkdownToHtml(markdowns.get(i), landscapes.get(i),
						activityName);
				combinedBody.append(extractBody(sectionHtml));
			}
			String mergedHtml = prepareHtmlForDocx(
					markdownToHtmlService.renderHtmlBodyToHtmlDocument(combinedBody.toString(),
							landscapes.get(0), activityName));
			byte[] rawDocx = libreOfficeConversionService.convertHtmlToDocx(mergedHtml.getBytes(StandardCharsets.UTF_8));
			return docxPostProcessor.processMerged(rawDocx, landscapes, activityName);
		} catch (Exception e) {
			logger.error("Failed to render merged markdown to DOCX: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to render merged markdown to DOCX: " + e.getMessage(), e);
		}
	}

	/**
	 * Prepare HTML for LibreOffice: remove iText running elements, add inline
	 * styles for table borders/colors and enforce font family on headings.
	 */
	private String prepareHtmlForDocx(String html) {
		// 1. Remove running header/footer divs
		String result = RUNNING_ELEMENT_PATTERN.matcher(html).replaceAll("");

		// 2. Add inline borders to table, th, td
		result = TABLE_TAG_PATTERN.matcher(result).replaceAll(match -> {
			String tag = match.group(1).toLowerCase();
			String border = "border: 1px solid #C8C8C8;";
			if ("table".equals(tag)) {
				border += " border-collapse: collapse;";
			}
			return "<" + match.group(1) + " style=\"" + border + "\"" + match.group(2);
		});

		// 3. Style thead th with background color, white text, white borders
		result = THEAD_PATTERN.matcher(result).replaceAll(theadMatch -> {
			String theadContent = theadMatch.group(1);
			String styled = TH_TAG_PATTERN.matcher(theadContent).replaceAll(
					"<th style=\"background-color: #2f70b3; color: #ffffff; border: 1px solid #ffffff; "
							+ "padding: 6pt; font-weight: bold; font-family: " + FONT_FAMILY + ";\"$1");
			return Matcher.quoteReplacement("<thead>" + styled + "</thead>");
		});

		// 4. Enforce sans-serif font on headings
		result = HEADING_TAG_PATTERN.matcher(result).replaceAll(
				"<$1 style=\"font-family: " + FONT_FAMILY + ";\"$2");

		return result;
	}

	private String extractBody(String html) {
		int bodyStart = html.indexOf("<body>");
		int bodyEnd = html.indexOf("</body>");
		if (bodyStart >= 0 && bodyEnd > bodyStart) {
			return html.substring(bodyStart + "<body>".length(), bodyEnd);
		}
		return html;
	}
}
