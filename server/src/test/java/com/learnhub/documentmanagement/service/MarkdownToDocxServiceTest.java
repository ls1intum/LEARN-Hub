package com.learnhub.documentmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

class MarkdownToDocxServiceTest {

	private MarkdownToDocxService service;

	@BeforeEach
	void setUp() {
		service = new MarkdownToDocxService(new MarkdownToHtmlService());
	}

	@Test
	void renderMarkdownToDocxWithPlainText() {
		String markdown = "This is plain text that should be rendered.";
		byte[] result = service.renderMarkdownToDocx(markdown);
		assertThat(result).isNotNull();
		assertThat(result.length).isGreaterThan(0);
		// DOCX is a ZIP file (starts with PK)
		assertThat(result[0]).isEqualTo((byte) 'P');
		assertThat(result[1]).isEqualTo((byte) 'K');
	}

	@Test
	void renderMarkdownToDocxWithHeadingsAndParagraphs() {
		String markdown = """
				# Main Title
				## Section One
				This is a paragraph with **bold** and *italic* text.
				### Subsection
				Another paragraph here.
				""";
		byte[] result = service.renderMarkdownToDocx(markdown);
		assertThat(result).isNotNull();
		assertThat(result.length).isGreaterThan(0);
	}

	@Test
	void renderMarkdownToDocxWithTable() {
		String markdown = """
				# Lesson Plan

				**Subject:** Mathematics
				**Grade:** 5

				| Time | Phase | Steps | Format | Skills | Materials |
				|------|-------|-------|--------|--------|-----------|
				| 5 min | Intro | Greeting | Plenary | Communication | - |
				| 20 min | Work | Solve tasks | Individual | Calculation | Worksheet |
				""";
		byte[] result = service.renderMarkdownToDocx(markdown);
		assertThat(result).isNotNull();
		assertThat(result.length).isGreaterThan(0);
	}

	@Test
	void renderMarkdownToDocxWithLists() {
		String markdown = """
				# Activity Plan

				Materials needed:
				- Whiteboard
				- Markers
				- Worksheets

				Steps:
				1. Introduction
				2. Main activity
				3. Wrap up
				""";
		byte[] result = service.renderMarkdownToDocx(markdown);
		assertThat(result).isNotNull();
		assertThat(result.length).isGreaterThan(0);
	}

	@Test
	void renderMarkdownToDocxWithCodeBlock() {
		String markdown = """
				# Example

				Here is some code:

				```
				function hello() {
				  return "world";
				}
				```
				""";
		byte[] result = service.renderMarkdownToDocx(markdown);
		assertThat(result).isNotNull();
		assertThat(result.length).isGreaterThan(0);
	}

	@Test
	void renderMarkdownToDocxWithBlockQuote() {
		String markdown = """
				# Quote

				> This is a block quote
				> with multiple lines.

				Regular text after.
				""";
		byte[] result = service.renderMarkdownToDocx(markdown);
		assertThat(result).isNotNull();
		assertThat(result.length).isGreaterThan(0);
	}

	@Test
	void renderMarkdownToDocxWithMixedContent() {
		String markdown = """
				# Full Document

				**Author:** John Doe
				**Date:** 2024-01-01

				## Introduction

				This is a paragraph with **bold**, *italic*, and `code` text.

				## Table Section

				| Column A | Column B |
				|----------|----------|
				| Value 1  | Value 2  |

				## Lists

				- Item one
				- Item two

				---

				> Final note
				""";
		byte[] result = service.renderMarkdownToDocx(markdown);
		assertThat(result).isNotNull();
		assertThat(result.length).isGreaterThan(0);
	}

	@Test
	void renderMarkdownToDocxIncludesHeaderAndFooterMetadata() throws Exception {
		byte[] result = service.renderMarkdownToDocx("# Header", true, "Binary Search Game");

		try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(result))) {
			assertThat(document.getHeaderList()).hasSize(1);
			assertThat(document.getFooterList()).hasSize(1);

			String headerText = document.getHeaderList().get(0).getText();
			String footerText = document.getFooterList().get(0).getText();

			assertThat(headerText).contains("Binary Search Game");
			assertThat(footerText).contains("LEARN-Hub");
			assertThat(footerText).contains("aet.cit.tum.de");
			assertThat(footerText).contains("Page");
		}
	}

	@Test
	void renderMarkdownToDocxWithRawHtmlBreaksAndHorizontalRules() {
		String markdown = """
				# Title

				First line<br>Second line<br>Third line

				<hr>

				Paragraph after rule.
				""";
		byte[] result = service.renderMarkdownToDocx(markdown);
		assertThat(result).isNotNull();
		assertThat(result.length).isGreaterThan(0);
		assertThat(result[0]).isEqualTo((byte) 'P');
		assertThat(result[1]).isEqualTo((byte) 'K');
	}

	@Test
	void renderMarkdownToDocxHeaderIncludesLogo() throws Exception {
		byte[] result = service.renderMarkdownToDocx("# Test", true, "My Activity");

		try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(result))) {
			assertThat(document.getHeaderList()).hasSize(1);
			assertThat(document.getHeaderList().get(0).getAllPictures()).isNotEmpty();
		}
	}

	@Test
	void renderMergedDocxProducesValidDocx() throws Exception {
		List<String> markdowns = List.of("# Deckblatt\n\nCover page content.", "# Artikulationsschema\n\n| A | B |\n|---|---|\n| 1 | 2 |",
				"# Hintergrundwissen\n\nBackground info.");
		List<Boolean> landscapes = List.of(false, true, false);

		byte[] result = service.renderMergedDocx(markdowns, landscapes, "My Activity");

		assertThat(result).isNotNull();
		assertThat(result[0]).isEqualTo((byte) 'P');
		assertThat(result[1]).isEqualTo((byte) 'K');

		try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(result))) {
			assertThat(doc.getHeaderList()).isNotEmpty();
			assertThat(doc.getFooterList()).isNotEmpty();
			String headerText = doc.getHeaderList().get(0).getText();
			assertThat(headerText).contains("My Activity");
		}
	}

	@Test
	void renderMergedDocxHasCorrectSectionOrientations() throws Exception {
		List<String> markdowns = List.of("# Portrait Section", "# Landscape Section", "# Portrait Section 2");
		List<Boolean> landscapes = List.of(false, true, false);

		byte[] result = service.renderMergedDocx(markdowns, landscapes, "Test");

		try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(result))) {
			// The body-level section (last section) should be portrait
			CTSectPr bodySectPr = doc.getDocument().getBody().getSectPr();
			assertThat(bodySectPr).isNotNull();
			assertThat(bodySectPr.getPgSz().getOrient().toString()).isEqualTo("portrait");
		}
	}

	@Test
	void renderMergedDocxRejectsMismatchedLists() {
		assertThatThrownBy(() -> service.renderMergedDocx(List.of("# A", "# B"), List.of(false), "Test"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void renderMarkdownToDocxTableHasHeaderStyling() throws Exception {
		String markdown = """
				| Header A | Header B |
				|----------|----------|
				| Cell 1   | Cell 2   |
				""";
		byte[] result = service.renderMarkdownToDocx(markdown, false, "");

		try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(result))) {
			List<XWPFTable> tables = doc.getTables();
			assertThat(tables).isNotEmpty();
			XWPFTable table = tables.get(0);
			XWPFTableRow headerRow = table.getRow(0);
			assertThat(headerRow).isNotNull();
			// Header row should have cells
			assertThat(headerRow.getTableCells()).hasSizeGreaterThanOrEqualTo(2);
			// Check that header cell text is present
			assertThat(headerRow.getCell(0).getText()).contains("Header A");
			assertThat(headerRow.getCell(1).getText()).contains("Header B");
		}
	}
}
