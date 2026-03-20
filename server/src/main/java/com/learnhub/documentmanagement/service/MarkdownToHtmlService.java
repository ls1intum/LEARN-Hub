package com.learnhub.documentmanagement.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Shared service for converting Markdown to styled HTML. Used by both
 * {@link MarkdownToPdfService} (via iText html2pdf) and
 * {@link MarkdownToDocxService} (via docx4j XHTMLImporter).
 */
@Service
public class MarkdownToHtmlService {

	private static final String HTML_TEMPLATE_PATH = "templates/markdown/html-document.html";
	private static final String DOCX_HTML_TEMPLATE_PATH = "templates/markdown/docx-document.html";
	private static final String CSS_TEMPLATE_PATH = "templates/markdown/pdf-styles.css";
	private static final String CSS_PORTRAIT_TEMPLATE_PATH = "templates/markdown/pdf-styles-portrait.css";
	private static final String DOCX_CSS_TEMPLATE_PATH = "templates/markdown/docx-styles.css";
	private static final String LOGO_PATH = "templates/markdown/header-logo.png";
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
	private static final Pattern ARTIKULATIONSSCHEMA_TITLE_PATTERN = Pattern.compile("^\\s*#\\s+Artikulationsschema\\b",
			Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

	/**
	 * Pattern to match HTML5 void elements that are NOT already self-closed.
	 * Matches {@code <br>
	 * }, {@code
	 *
	<hr>
	 * }, {@code <img ...>} etc. but not {@code <br />
	 * } or {@code <br/>
	 * }. The negative lookbehind {@code (?<!/)} before {@code >} ensures already
	 * self-closed tags are excluded.
	 */
	private static final Pattern VOID_ELEMENT_PATTERN = Pattern
			.compile("<(br|hr|img|input|col|source|track|wbr)(\\s[^>]*?)?(?<!/)>", Pattern.CASE_INSENSITIVE);

	private final Parser parser;
	private final HtmlRenderer renderer;
	private final String htmlTemplate;
	private final String docxHtmlTemplate;
	private final String cssTemplate;
	private final String cssPortraitTemplate;
	private final String docxCssTemplate;
	private final String logoDataUri;

	public MarkdownToHtmlService() {
		var extensions = List.of(TablesExtension.create());
		this.parser = Parser.builder().extensions(extensions).build();
		this.renderer = HtmlRenderer.builder().extensions(extensions).build();
		this.htmlTemplate = loadTemplate(HTML_TEMPLATE_PATH);
		this.docxHtmlTemplate = loadTemplate(DOCX_HTML_TEMPLATE_PATH);
		this.cssTemplate = loadTemplate(CSS_TEMPLATE_PATH);
		this.cssPortraitTemplate = loadTemplate(CSS_PORTRAIT_TEMPLATE_PATH);
		this.docxCssTemplate = loadTemplate(DOCX_CSS_TEMPLATE_PATH);
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
		String body = renderDecoratedBody(markdown, false);
		String css = landscape ? cssTemplate : cssPortraitTemplate;
		String name = activityName != null ? activityName : "";
		String downloadDate = LocalDateTime.now().format(DATE_FORMATTER);
		return htmlTemplate.replace("{{styles}}", css).replace("{{body}}", body).replace("{{activityName}}", name)
				.replace("{{logoDataUri}}", logoDataUri).replace("{{downloadDate}}", downloadDate);
	}

	/**
	 * Convert Markdown to a styled HTML document suitable for DOCX conversion via
	 * docx4j XHTMLImporter. Uses a DOCX-specific template without @page rules or
	 * running header/footer elements (those are added via the DOCX API).
	 *
	 * <p>
	 * The output is sanitized to XHTML: void elements like {@code <br>
	 * }, {@code
	 *
	<hr>
	 * }, and {@code <img>} are converted to self-closing form so that docx4j's XML
	 * parser can process them.
	 * </p>
	 *
	 * @param markdown
	 *            the markdown content
	 */
	public String renderMarkdownToDocxHtml(String markdown) {
		String body = renderDecoratedBody(markdown, true);
		String html = docxHtmlTemplate.replace("{{styles}}", docxCssTemplate).replace("{{body}}", body);
		return sanitizeToXhtml(html);
	}

	/**
	 * Parse Markdown into a CommonMark AST node.
	 */
	public Node parseToNode(String markdown) {
		return parser.parse(markdown);
	}

	/**
	 * Sanitize HTML to XHTML by converting HTML5 void elements (e.g. {@code <br>
	 * }, {@code
	 *
	<hr>
	 * }, {@code <img ...>}) to self-closing XHTML form ({@code <br />
	 * }, {@code
	 *
	<hr />
	 * }, {@code <img ... />}). This is required because docx4j's
	 * {@link org.docx4j.convert.in.xhtml.XHTMLImporterImpl} parses HTML as strict
	 * XML, which rejects unclosed void elements.
	 */
	private String sanitizeToXhtml(String html) {
		return VOID_ELEMENT_PATTERN.matcher(html).replaceAll(match -> {
			String tag = match.group(1);
			String attrs = match.group(2);
			return "<" + tag + (attrs != null ? attrs : "") + " />";
		});
	}

	private String decorateRenderedBody(String markdown, String body, boolean forDocx) {
		if (markdown == null || body == null) {
			return body;
		}
		if (!ARTIKULATIONSSCHEMA_TITLE_PATTERN.matcher(markdown).find()) {
			return body;
		}
		if (forDocx) {
			return body.replaceFirst("<table>",
					"<table class=\"artikulationsschema-table\" style=\"width:100%; table-layout:fixed;\">");
		}
		return body.replaceFirst("<table>", "<table class=\"artikulationsschema-table\">");
	}

	private String renderDecoratedBody(String markdown, boolean forDocx) {
		Node document = parser.parse(markdown != null ? markdown : "");
		return decorateRenderedBody(markdown, renderer.render(document), forDocx);
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
