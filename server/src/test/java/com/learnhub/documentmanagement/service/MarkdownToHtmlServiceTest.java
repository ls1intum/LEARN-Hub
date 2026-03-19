package com.learnhub.documentmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarkdownToHtmlServiceTest {

	private MarkdownToHtmlService service;

	@BeforeEach
	void setUp() {
		service = new MarkdownToHtmlService();
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
	void renderMarkdownToDocxHtmlSanitizesVoidElements() {
		String markdown = "Line one<br>Line two<br>Line three\n\n<hr>\n\nDone.";
		String html = service.renderMarkdownToDocxHtml(markdown);
		assertThat(html).doesNotContain("<br>").doesNotContain("<hr>");
		assertThat(html).contains("<br />");
		assertThat(html).contains("<hr />");
	}

	@Test
	void renderMarkdownToDocxHtmlPreservesAlreadySelfClosedElements() {
		String markdown = "Line one  \nLine two";
		String html = service.renderMarkdownToDocxHtml(markdown);
		assertThat(html).contains("<br />");
		assertThat(html).doesNotContain("<br>");
	}
}
