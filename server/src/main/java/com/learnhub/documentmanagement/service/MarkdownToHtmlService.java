package com.learnhub.documentmanagement.service;

import com.learnhub.service.SanitizationService;
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
import org.springframework.beans.factory.annotation.Autowired;
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

	private static final Pattern TAFELBILD_TITLE_PATTERN = Pattern.compile("^\\s*#\\s+Tafelbild\\b",
			Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	private static final Pattern TAFELBILD_HTML_TITLE_PATTERN = Pattern.compile("<h1[^>]*>\\s*Tafelbild\\s*</h1>",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern ARTIKULATIONSSCHEMA_TITLE_PATTERN = Pattern.compile("^\\s*#\\s+Artikulationsschema\\b",
			Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	private static final Pattern ARTIKULATIONSSCHEMA_HTML_TITLE_PATTERN = Pattern
			.compile("<h1[^>]*>\\s*Artikulationsschema\\s*</h1>", Pattern.CASE_INSENSITIVE);
	private static final Pattern TABLE_OPENING_PATTERN = Pattern.compile("<table(\\s+[^>]*)?>",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern CLASS_ATTRIBUTE_PATTERN = Pattern.compile("\\bclass\\s*=\\s*([\"'])(.*?)\\1",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern PARAGRAPH_IMG_PATTERN = Pattern.compile("<p>\\s*<img((?:\\s+[^>]*)?)(/?)>\\s*</p>",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	// A base64 markdown image, optionally preceded by its learnhub annotation
	// comment. When an LLM wraps such an image in an HTML block (e.g. a centering
	// <div>), CommonMark treats the whole block as raw HTML and emits the image as
	// literal markdown text. Forcing blank lines around it splits the image into
	// its own block so it renders as an image. (HTML <img> tags already render
	// inside HTML blocks, so only markdown image syntax needs this.)
	private static final Pattern EMBEDDED_DATA_IMAGE_PATTERN = Pattern
			.compile("(?:<!--\\s*learnhub-image[^>]*-->\\s*)?!\\[[^\\]]*\\]\\(data:[^)]+\\)", Pattern.DOTALL);
	private static final Pattern EXCESS_BLANK_LINES_PATTERN = Pattern.compile("\\n{3,}");

	private final Parser parser;
	private final HtmlRenderer renderer;
	private final String htmlTemplate;
	private final String commonCssTemplate;
	private final String pdfCssTemplate;
	private final String pdfCssPortraitTemplate;
	private final String logoDataUri;

	@Autowired
	private SanitizationService sanitizationService;

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
		return renderMarkdownToHtml(markdown, landscape, activityName, false);
	}

	/**
	 * Convert Markdown to a complete, styled HTML document, optionally applying the
	 * exercise-sheet layout (outer border with Name / Datum fields at the top).
	 *
	 * @param markdown
	 *            the markdown content
	 * @param landscape
	 *            true for landscape, false for portrait
	 * @param activityName
	 *            the activity name shown in the page header
	 * @param exerciseSheet
	 *            when {@code true} the body is wrapped in an outer border with
	 *            student Name / Datum fill-in fields at the top
	 */
	public String renderMarkdownToHtml(String markdown, boolean landscape, String activityName, boolean exerciseSheet) {
		return renderMarkdownToHtml(markdown, landscape, activityName, exerciseSheet, false);
	}

	public String renderMarkdownToHtml(String markdown, boolean landscape, String activityName, boolean exerciseSheet,
			boolean isBoardImage) {
		String normalizedMarkdown = normalizeMarkdown(markdown);
		String body = renderDecoratedMarkdownBody(normalizedMarkdown);
		if (isBoardImage) {
			body = PARAGRAPH_IMG_PATTERN.matcher(body).replaceAll(
					"<div style=\"width:100%;margin:10pt 0;page-break-inside:avoid\"><img$1 style=\"display:block;width:100%;height:auto;max-height:none\"$2></div>");
		}
		String bodyClass = isBoardImage ? "tafelbild-render" : detectDocumentBodyClass(normalizedMarkdown, body);
		return wrapPdfHtmlDocument(body, landscape, activityName, bodyClass, exerciseSheet);
	}

	/**
	 * Wrap an existing HTML body in the styled PDF document template.
	 */
	public String renderHtmlBodyToHtmlDocument(String htmlBody, boolean landscape, String activityName) {
		String body = decorateHtmlBody(htmlBody);
		return wrapPdfHtmlDocument(body, landscape, activityName, detectDocumentBodyClass("", body));
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

	// Height (pt) of the Name/Datum fill-in row drawn by the iText event handler.
	// Must stay in sync with ExerciseSheetEventHandler.EXTRA_TOP_MARGIN.
	static final float EXERCISE_SHEET_EXTRA_TOP_MARGIN_PT = 22f;
	// Gap (pt) between the LEARN-Hub logo area and the top edge of the border
	// rectangle.
	// Must stay in sync with ExerciseSheetEventHandler.BORDER_TOP_INSET.
	private static final float EXERCISE_BORDER_TOP_INSET_PT = 4f;
	// Gap (pt) between the Name/Datum separator line and the first content line.
	private static final float EXERCISE_CONTENT_TOP_GAP_PT = 5f;
	// Extra padding (pt) inside the border on the left, right, and bottom.
	private static final float EXERCISE_INNER_PADDING_PT = 6f;
	private static final float STANDARD_TOP_MARGIN_PT = 55f;

	private String wrapPdfHtmlDocument(String body, boolean landscape, String activityName, String bodyClass) {
		return wrapPdfHtmlDocument(body, landscape, activityName, bodyClass, false);
	}

	private String wrapPdfHtmlDocument(String body, boolean landscape, String activityName, String bodyClass,
			boolean exerciseSheet) {
		String pageMargins;
		if (exerciseSheet) {
			int top = Math.round(STANDARD_TOP_MARGIN_PT + EXERCISE_BORDER_TOP_INSET_PT
					+ EXERCISE_SHEET_EXTRA_TOP_MARGIN_PT + EXERCISE_CONTENT_TOP_GAP_PT);
			float baseSide = landscape ? 30f : 35f;
			float baseBottom = landscape ? 50f : 55f;
			int side = Math.round(baseSide + EXERCISE_INNER_PADDING_PT);
			int bottom = Math.round(baseBottom + EXERCISE_INNER_PADDING_PT);
			pageMargins = top + "pt " + side + "pt " + bottom + "pt " + side + "pt";
		} else {
			pageMargins = landscape ? "55pt 30pt 50pt 30pt" : "55pt 35pt 55pt 35pt";
		}
		String normalizedBodyClass = bodyClass == null ? "" : bodyClass.trim();
		String downloadDate = LocalDateTime.now().format(DATE_FORMATTER);
		String css = (landscape ? pdfCssTemplate : pdfCssPortraitTemplate).replace("{{page_margins}}", pageMargins)
				+ "\n\n" + commonCssTemplate;
		String name = activityName != null ? activityName : "";
		String bodyClassAttribute = normalizedBodyClass.isEmpty() ? "" : " class=\"" + normalizedBodyClass + "\"";
		return htmlTemplate.replace("{{styles}}", css).replace("{{body}}", body).replace("{{activityName}}", name)
				.replace("{{logoDataUri}}", logoDataUri).replace("{{downloadDate}}", downloadDate)
				.replace("{{bodyClassAttribute}}", bodyClassAttribute);
	}

	private String renderDecoratedMarkdownBody(String markdown) {
		Node document = parser.parse(markdown);
		String body = renderer.render(document);
		if (!ARTIKULATIONSSCHEMA_TITLE_PATTERN.matcher(markdown).find()) {
			return body;
		}
		return addArtikulationsschemaTableClass(body);
	}

	private String normalizeMarkdown(String markdown) {
		return separateEmbeddedImages(sanitizationService.sanitize(markdown));
	}

	/**
	 * Ensure base64 markdown images are separated from surrounding content by blank
	 * lines, so an image wrapped in an HTML block (e.g. a centering {@code <div>})
	 * is rendered as an image rather than literal markdown text.
	 */
	private String separateEmbeddedImages(String markdown) {
		if (markdown == null || markdown.isEmpty()) {
			return markdown;
		}
		String padded = EMBEDDED_DATA_IMAGE_PATTERN.matcher(markdown)
				.replaceAll(match -> "\n\n" + match.group() + "\n\n");
		return EXCESS_BLANK_LINES_PATTERN.matcher(padded).replaceAll("\n\n");
	}

	private String decorateHtmlBody(String htmlBody) {
		String normalizedHtml = sanitizationService.sanitize(htmlBody);
		if (!ARTIKULATIONSSCHEMA_HTML_TITLE_PATTERN.matcher(normalizedHtml).find()) {
			return normalizedHtml;
		}
		return addArtikulationsschemaTableClass(normalizedHtml);
	}

	private String detectDocumentBodyClass(String markdown, String htmlBody) {
		if ((markdown != null && TAFELBILD_TITLE_PATTERN.matcher(markdown).find())
				|| (htmlBody != null && TAFELBILD_HTML_TITLE_PATTERN.matcher(htmlBody).find())) {
			return "tafelbild-render";
		}
		return "";
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
