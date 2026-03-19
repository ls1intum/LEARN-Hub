package com.learnhub.documentmanagement.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

	private final Parser parser;
	private final HtmlRenderer renderer;
	private final String htmlTemplate;
	private final String cssTemplate;
	private final String cssPortraitTemplate;

	public MarkdownToHtmlService() {
		var extensions = List.of(TablesExtension.create());
		this.parser = Parser.builder().extensions(extensions).build();
		this.renderer = HtmlRenderer.builder().extensions(extensions).build();
		this.htmlTemplate = loadTemplate(HTML_TEMPLATE_PATH);
		this.cssTemplate = loadTemplate(CSS_TEMPLATE_PATH);
		this.cssPortraitTemplate = loadTemplate(CSS_PORTRAIT_TEMPLATE_PATH);
	}

	/**
	 * Convert Markdown to a complete, styled HTML document suitable for PDF
	 * conversion via iText html2pdf. Uses landscape orientation by default.
	 */
	public String renderMarkdownToHtml(String markdown) {
		return renderMarkdownToHtml(markdown, true);
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
		Node document = parser.parse(markdown);
		String body = renderer.render(document);
		String css = landscape ? cssTemplate : cssPortraitTemplate;
		return htmlTemplate.replace("{{styles}}", css).replace("{{body}}", body);
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
}
