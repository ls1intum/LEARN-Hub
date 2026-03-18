package com.learnhub.documentmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarkdownToPdfServiceTest {

	private MarkdownToPdfService service;

	@BeforeEach
	void setUp() {
		service = new MarkdownToPdfService(new MarkdownToHtmlService());
	}

	@Test
	void renderMarkdownToPdfWithPlainText() {
		String markdown = "This is plain text that should be rendered.";
		byte[] result = service.renderMarkdownToPdf(markdown);
		assertThat(result).isNotNull();
		assertThat(result.length).isGreaterThan(0);
		// PDF starts with %PDF
		assertThat(new String(result, 0, 5)).startsWith("%PDF");
	}

	@Test
	void renderMarkdownToPdfWithHeadingsAndParagraphs() {
		String markdown = """
				# Main Title
				## Section One
				This is a paragraph with **bold** and *italic* text.
				### Subsection
				Another paragraph here.
				""";
		byte[] result = service.renderMarkdownToPdf(markdown);
		assertThat(result).isNotNull();
		assertThat(result.length).isGreaterThan(0);
	}

	@Test
	void renderMarkdownToPdfWithTable() {
		String markdown = """
				# Lesson Plan

				**Subject:** Mathematics
				**Grade:** 5

				| Time | Phase | Steps | Format | Skills | Materials |
				|------|-------|-------|--------|--------|-----------|
				| 5 min | Intro | Greeting | Plenary | Communication | - |
				| 20 min | Work | Solve tasks | Individual | Calculation | Worksheet |
				""";
		byte[] result = service.renderMarkdownToPdf(markdown);
		assertThat(result).isNotNull();
		assertThat(result.length).isGreaterThan(0);
	}

	@Test
	void renderMarkdownToPdfWithLists() {
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
		byte[] result = service.renderMarkdownToPdf(markdown);
		assertThat(result).isNotNull();
		assertThat(result.length).isGreaterThan(0);
	}

	@Test
	void renderMarkdownToPdfWithCodeBlock() {
		String markdown = """
				# Example

				Here is some code:

				```
				function hello() {
				  return "world";
				}
				```
				""";
		byte[] result = service.renderMarkdownToPdf(markdown);
		assertThat(result).isNotNull();
		assertThat(result.length).isGreaterThan(0);
	}

	@Test
	void renderMarkdownToPdfWithBlockQuote() {
		String markdown = """
				# Quote

				> This is a block quote
				> with multiple lines.

				Regular text after.
				""";
		byte[] result = service.renderMarkdownToPdf(markdown);
		assertThat(result).isNotNull();
		assertThat(result.length).isGreaterThan(0);
	}

	@Test
	void renderMarkdownToPdfWithMixedContent() {
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
		byte[] result = service.renderMarkdownToPdf(markdown);
		assertThat(result).isNotNull();
		assertThat(result.length).isGreaterThan(0);
	}
}
