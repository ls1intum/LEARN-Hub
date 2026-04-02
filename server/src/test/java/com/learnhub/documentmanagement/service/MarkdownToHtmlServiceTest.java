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
	void renderMarkdownToHtmlSanitizesAiTypography() {
		String html = service.renderMarkdownToHtml("AI\u2014Mensch\u2026\u00A0\u201CSchule\u201D\u200B");
		assertThat(html).contains("AI-Mensch... &quot;Schule&quot;");
		assertThat(html).doesNotContain("\u2014");
		assertThat(html).doesNotContain("\u2026");
		assertThat(html).doesNotContain("\u00A0");
		assertThat(html).doesNotContain("\u200B");
	}
}
