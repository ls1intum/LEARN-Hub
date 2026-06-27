package com.learnhub.documentmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.learnhub.service.SanitizationService;
import org.commonmark.node.Paragraph;
import org.commonmark.node.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MarkdownToHtmlServiceTest {

	private MarkdownToHtmlService service;

	@BeforeEach
	void setUp() {
		service = new MarkdownToHtmlService();
		ReflectionTestUtils.setField(service, "sanitizationService", new SanitizationService());
	}

	@Test
	void renderMarkdownToHtmlContainsHtmlStructure() {
		String html = service.renderMarkdownToHtml("Hello");
		assertThat(html).contains("<!DOCTYPE html>").contains("<html>").contains("</html>").contains("<body>");
	}

	@Test
	void renderMarkdownToHtmlConvertsHeading() {
		String html = service.renderMarkdownToHtml("# Title");
		assertThat(html).contains("<h1>Title</h1>");
	}

	@Test
	void renderMarkdownToHtmlConvertsTable() {
		String markdown = """
				| A | B |
				|---|---|
				| 1 | 2 |
				""";
		String html = service.renderMarkdownToHtml(markdown);
		assertThat(html).contains("<table>").contains("<thead>").contains("<tbody>");
	}

	@Test
	void renderMarkdownToHtmlContainsCssStyles() {
		String html = service.renderMarkdownToHtml("text");
		assertThat(html).contains("<style>").contains("@page").contains("A4 landscape");
	}

	@Test
	void renderMarkdownToHtmlLimitsImageWidthAndLandscapeHeight() {
		String html = service.renderMarkdownToHtml("![Bild](data:image/png;base64,ZmFrZQ==)");
		assertThat(html).contains("max-width: 100%").contains("max-height: 43mm");
	}

	@Test
	void renderMarkdownToHtmlPreservesBase64ImageAsImgTag() {
		// A dummy embedded image must survive sanitization and render as a real
		// <img> with its data URI intact (not stripped or escaped to text).
		String html = service.renderMarkdownToHtml("![Diagramm](data:image/png;base64,ZmFrZQ==)");
		assertThat(html).contains("<img").contains("src=\"data:image/png;base64,ZmFrZQ==\"")
				.doesNotContain("![Diagramm]");
	}

	@Test
	void renderMarkdownToHtmlTafelbildRendersImagesUnbounded() {
		// Tafelbild mode lifts the per-image height cap so the generated board
		// image fills the slide; the body is tagged for tafelbild-specific styling.
		String html = service.renderMarkdownToHtml("# Tafelbild\n\n![Bild](data:image/png;base64,ZmFrZQ==)", true, "",
				false, true);
		assertThat(html).contains("<body class=\"tafelbild-render\">").contains("<h1>Tafelbild</h1>")
				.contains("max-height:none");
	}

	@Test
	void renderMarkdownToHtmlAutoDetectsTafelbildBodyClassFromHeading() {
		String html = service.renderMarkdownToHtml("# Tafelbild\n\nInhalt");
		assertThat(html).contains("<body class=\"tafelbild-render\">").contains("<h1>Tafelbild</h1>");
	}

	@Test
	void renderMarkdownToHtmlLimitsPortraitImageHeight() {
		String html = service.renderMarkdownToHtml("![Bild](data:image/png;base64,ZmFrZQ==)", false);
		assertThat(html).contains("max-height: 64mm");
	}

	@Test
	void parseToNodeReturnsNonNull() {
		var node = service.parseToNode("# Hello\n\nParagraph.");
		assertThat(node).isNotNull();
		assertThat(node.getFirstChild()).isNotNull();
	}

	@Test
	void parseToNodeSanitizesAiTypographyBeforeParsing() {
		var node = service.parseToNode("AI\u2014Schule\u2026 \u201CHallo\u201D");
		assertThat(node).isNotNull();
		assertThat(node.getFirstChild()).isInstanceOf(Paragraph.class);
		assertThat(node.getFirstChild().getFirstChild()).isInstanceOf(Text.class);
		assertThat(((Text) node.getFirstChild().getFirstChild()).getLiteral()).isEqualTo("AI-Schule... \"Hallo\"");
	}

	@Test
	void renderMarkdownToHtmlRendersImageWrappedInHtmlDiv() {
		String markdown = """
				<div style="text-align:center">
				<!-- learnhub-image:id=fig1; prompt=a cat -->
				![Bild](data:image/png;base64,ZmFrZQ==)
				</div>""";
		String html = service.renderMarkdownToHtml(markdown);
		// The image renders as an <img> rather than literal markdown text.
		assertThat(html).contains("<img src=\"data:image/png;base64,ZmFrZQ==\"");
		assertThat(html).doesNotContain("![Bild]");
	}

	@Test
	void renderMarkdownToHtmlSanitizesAiTypography() {
		String html = service.renderMarkdownToHtml("AI\u2014Mensch\u2026\u00A0\u201CSchule\u201D\u200B");
		assertThat(html).contains("AI-Mensch... &quot;Schule&quot;");
		assertThat(html).doesNotContain("\u2014");
		assertThat(html).doesNotContain("\u2026");
		assertThat(html).doesNotContain("\u00A0");
		assertThat(html).doesNotContain("\u200B");
	}
}
