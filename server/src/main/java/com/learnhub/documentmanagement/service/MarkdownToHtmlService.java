package com.learnhub.documentmanagement.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Shared service for converting Markdown to styled HTML. Used by both
 * {@link MarkdownToPdfService} (via iText html2pdf) and
 * {@link MarkdownToDocxService} (for shared AST parsing).
 */
@Service
public class MarkdownToHtmlService {

	private static final String HTML_TEMPLATE_PATH = "templates/markdown/html-document.html";
	private static final String CSS_TEMPLATE_PATH = "templates/markdown/pdf-styles.css";
	private static final String CSS_PORTRAIT_TEMPLATE_PATH = "templates/markdown/pdf-styles-portrait.css";
	private static final String LOGO_PATH = "templates/markdown/header-logo.png";
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

	private final Parser parser;
	private final HtmlRenderer renderer;
	private final String htmlTemplate;
	private final String cssTemplate;
	private final String cssPortraitTemplate;
	private final String logoDataUri;

	public MarkdownToHtmlService() {
		var extensions = List.of(TablesExtension.create());
		this.parser = Parser.builder().extensions(extensions).build();
		this.renderer = HtmlRenderer.builder().extensions(extensions).build();
		this.htmlTemplate = loadTemplate(HTML_TEMPLATE_PATH);
		this.cssTemplate = loadTemplate(CSS_TEMPLATE_PATH);
		this.cssPortraitTemplate = loadTemplate(CSS_PORTRAIT_TEMPLATE_PATH);
		this.logoDataUri = loadLogoAsDataUri();
	}

	/**
	 * Convert Markdown to a complete, styled HTML document suitable for PDF
	 * conversion via iText html2pdf. Uses landscape orientation by default.
	 */
	public String renderMarkdownToHtml(String markdown) {
		return renderMarkdownToHtml(markdown, true, "");
	}

	/**
	 * Convert Markdown to a complete, styled HTML document with specified
	 * orientation.
	 *
	 * @param markdown
	 *            the markdown content
	 * @param landscape
	 *            true for landscape, false for portrait
	 */
	public String renderMarkdownToHtml(String markdown, boolean landscape) {
		return renderMarkdownToHtml(markdown, landscape, "");
	}

	/**
	 * Convert Markdown to a complete, styled HTML document with specified
	 * orientation, activity name in header, and download date in footer.
	 *
	 * @param markdown
	 *            the markdown content
	 * @param landscape
	 *            true for landscape, false for portrait
	 * @param activityName
	 *            the activity name shown in the page header
	 */
	public String renderMarkdownToHtml(String markdown, boolean landscape, String activityName) {
		Node document = parser.parse(markdown);
		String body = renderer.render(document);
		String css = landscape ? cssTemplate : cssPortraitTemplate;
		String name = activityName != null ? activityName : "";
		String downloadDate = LocalDateTime.now().format(DATE_FORMATTER);
		return htmlTemplate.replace("{{styles}}", css).replace("{{body}}", body)
				.replace("{{activityName}}", name).replace("{{logoDataUri}}", logoDataUri)
				.replace("{{downloadDate}}", downloadDate);
	}

	/**
	 * Parse Markdown into a CommonMark AST node. Used by services that need the AST
	 * for further processing (e.g. DOCX generation via Apache POI).
	 */
	public Node parseToNode(String markdown) {
		return parser.parse(markdown);
	}

	private String loadTemplate(String path) {
		ClassPathResource resource = new ClassPathResource(path);
		try (InputStream inputStream = resource.getInputStream()) {
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load template resource: " + path, e);
		}
	}

	private String loadLogoAsDataUri() {
		ClassPathResource resource = new ClassPathResource(LOGO_PATH);
		try (InputStream inputStream = resource.getInputStream()) {
			byte[] imageBytes = inputStream.readAllBytes();
			String base64 = Base64.getEncoder().encodeToString(imageBytes);
			return "data:image/png;base64," + base64;
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load logo image: " + LOGO_PATH, e);
		}
	}
}
