package com.learnhub.documentmanagement.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Shared service for converting Markdown to styled HTML. Used by
 * {@link MarkdownToPdfService} (via iText html2pdf) for PDF generation.
 */
@Service
public class MarkdownToHtmlService {

	private static final String HTML_TEMPLATE_PATH = "templates/markdown/html-document.html";
	private static final String COMMON_CSS_TEMPLATE_PATH = "templates/markdown/common-styles.css";
	private static final String PDF_CSS_TEMPLATE_PATH = "templates/markdown/pdf-styles.css";
	private static final String PDF_CSS_PORTRAIT_TEMPLATE_PATH = "templates/markdown/pdf-styles-portrait.css";
	private static final String LOGO_PATH = "templates/markdown/header-logo.png";
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
	private static final Pattern ARTIKULATIONSSCHEMA_TITLE_PATTERN = Pattern.compile("^\\s*#\\s+Artikulationsschema\\b",
			Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	private static final Pattern ARTIKULATIONSSCHEMA_HTML_TITLE_PATTERN = Pattern
			.compile("<h1[^>]*>\\s*Artikulationsschema\\s*</h1>", Pattern.CASE_INSENSITIVE);
	private static final Pattern TABLE_OPENING_PATTERN = Pattern.compile("<table(\\s+[^>]*)?>", Pattern.CASE_INSENSITIVE);
	private static final Pattern CLASS_ATTRIBUTE_PATTERN = Pattern.compile("\\bclass\\s*=\\s*([\"'])(.*?)\\1",
			Pattern.CASE_INSENSITIVE);

	private final Parser parser;
	private final HtmlRenderer renderer;
	private final String htmlTemplate;
	private final String commonCssTemplate;
	private final String pdfCssTemplate;
	private final String pdfCssPortraitTemplate;
	private final String logoDataUri;

	public MarkdownToHtmlService() {
		var extensions = List.of(TablesExtension.create());
		this.parser = Parser.builder().extensions(extensions).build();
		this.renderer = HtmlRenderer.builder().extensions(extensions).build();
		this.htmlTemplate = loadTemplate(HTML_TEMPLATE_PATH);
		this.commonCssTemplate = loadTemplate(COMMON_CSS_TEMPLATE_PATH);
		this.pdfCssTemplate = loadTemplate(PDF_CSS_TEMPLATE_PATH);
		this.pdfCssPortraitTemplate = loadTemplate(PDF_CSS_PORTRAIT_TEMPLATE_PATH);
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
		String body = renderDecoratedMarkdownBody(markdown);
		return wrapPdfHtmlDocument(body, landscape, activityName);
	}

	/**
	 * Wrap an existing HTML body in the styled PDF document template.
	 */
	public String renderHtmlBodyToHtmlDocument(String htmlBody, boolean landscape, String activityName) {
		String body = decorateHtmlBody(htmlBody);
		return wrapPdfHtmlDocument(body, landscape, activityName);
	}

	/**
	 * Return the LEARN-Hub logo as a base64-encoded data URI suitable for embedding
	 * in HTML {@code <img>} tags.
	 */
	public String getLogoDataUri() {
		return logoDataUri;
	}

	/**
	 * Parse Markdown into a CommonMark AST node.
	 */
	public Node parseToNode(String markdown) {
		return parser.parse(normalizeMarkdown(markdown));
	}

	private String wrapPdfHtmlDocument(String body, boolean landscape, String activityName) {
		String css = (landscape ? pdfCssTemplate : pdfCssPortraitTemplate) + "\n\n" + commonCssTemplate;
		String name = activityName != null ? activityName : "";
		String downloadDate = LocalDateTime.now().format(DATE_FORMATTER);
		return htmlTemplate.replace("{{styles}}", css).replace("{{body}}", body).replace("{{activityName}}", name)
				.replace("{{logoDataUri}}", logoDataUri).replace("{{downloadDate}}", downloadDate);
	}

	private String renderDecoratedMarkdownBody(String markdown) {
		String normalizedMarkdown = normalizeMarkdown(markdown);
		Node document = parser.parse(normalizedMarkdown);
		String body = renderer.render(document);
		if (!ARTIKULATIONSSCHEMA_TITLE_PATTERN.matcher(normalizedMarkdown).find()) {
			return body;
		}
		return addArtikulationsschemaTableClass(body);
	}

	private String normalizeMarkdown(String markdown) {
		return normalizeTypography(markdown);
	}

	private String decorateHtmlBody(String htmlBody) {
		String normalizedHtml = normalizeTypography(htmlBody);
		if (!ARTIKULATIONSSCHEMA_HTML_TITLE_PATTERN.matcher(normalizedHtml).find()) {
			return normalizedHtml;
		}
		return addArtikulationsschemaTableClass(normalizedHtml);
	}

	private String addArtikulationsschemaTableClass(String body) {
		if (body == null || body.isBlank()) {
			return body;
		}

		Matcher tableMatcher = TABLE_OPENING_PATTERN.matcher(body);
		if (!tableMatcher.find()) {
			return body;
		}

		String openingTag = tableMatcher.group();
		String updatedOpeningTag = appendCssClass(openingTag, "artikulationsschema-table");
		return body.substring(0, tableMatcher.start()) + updatedOpeningTag + body.substring(tableMatcher.end());
	}

	private String appendCssClass(String tag, String cssClass) {
		Matcher classMatcher = CLASS_ATTRIBUTE_PATTERN.matcher(tag);
		if (!classMatcher.find()) {
			return tag.replaceFirst("<table", "<table class=\"" + cssClass + "\"");
		}

		String classes = classMatcher.group(2);
		if (Pattern.compile("(^|\\s)" + Pattern.quote(cssClass) + "(\\s|$)").matcher(classes).find()) {
			return tag;
		}

		String quote = classMatcher.group(1);
		String replacement = "class=" + quote + classes.strip() + " " + cssClass + quote;
		return classMatcher.replaceFirst(Matcher.quoteReplacement(replacement));
	}

	private String normalizeTypography(String content) {
		if (content == null || content.isEmpty()) {
			return "";
		}

		return content.replace("&ndash;", "-").replace("&#8211;", "-").replace("&#x2013;", "-")
				.replace("&mdash;", "-").replace("&#8212;", "-").replace("&#x2014;", "-").replace('\u2010', '-')
				.replace('\u2011', '-').replace('\u2012', '-').replace('\u2013', '-').replace('\u2014', '-')
				.replace('\u2015', '-').replace('\u2212', '-');
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
