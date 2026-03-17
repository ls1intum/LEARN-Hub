package com.learnhub.documentmanagement.service;

import java.util.List;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;

/**
 * Shared service for converting Markdown to styled HTML. Used by both
 * {@link MarkdownToPdfService} (via iText html2pdf) and
 * {@link MarkdownToDocxService} (for shared AST parsing).
 */
@Service
public class MarkdownToHtmlService {

	private static final String HTML_STYLES = """
			body {
			    font-family: Helvetica, Arial, sans-serif;
			    font-size: 11pt;
			    margin: 0;
			    padding: 0;
			}
			h1 { font-size: 18pt; text-align: center; margin-top: 0; margin-bottom: 10pt; }
			h2 { font-size: 15pt; margin-top: 12pt; margin-bottom: 6pt; }
			h3 { font-size: 13pt; margin-top: 10pt; margin-bottom: 4pt; }
			h4, h5, h6 { font-size: 11pt; margin-top: 8pt; margin-bottom: 4pt; }
			p { margin-bottom: 6pt; }
			table {
			    width: 100%;
			    border-collapse: collapse;
			    margin-bottom: 10pt;
			    font-size: 9pt;
			}
			thead tr { background-color: #29417A; color: #ffffff; }
			thead th {
			    padding: 6pt;
			    font-size: 10pt;
			    font-weight: bold;
			    border: 0.5pt solid #ffffff;
			}
			tbody tr:nth-child(even) { background-color: #F0F4FA; }
			tbody td { padding: 5pt; border: 0.5pt solid #C8C8C8; }
			code {
			    font-family: Courier, monospace;
			    font-size: 9pt;
			    background-color: #F0F0F0;
			    padding: 1pt 2pt;
			}
			pre {
			    font-family: Courier, monospace;
			    font-size: 9pt;
			    background-color: #F5F5F5;
			    padding: 8pt;
			    margin-bottom: 8pt;
			    white-space: pre-wrap;
			}
			pre code { background-color: transparent; padding: 0; }
			blockquote {
			    border-left: 2pt solid #B4B4B4;
			    padding-left: 10pt;
			    font-style: italic;
			    color: #646464;
			    margin-bottom: 6pt;
			}
			a { color: #0563C1; text-decoration: underline; }
			hr { border: none; border-bottom: 0.5pt solid #000000; margin-bottom: 10pt; }
			ul, ol { margin-bottom: 6pt; }
			""";

	private final Parser parser;
	private final HtmlRenderer renderer;

	public MarkdownToHtmlService() {
		var extensions = List.of(TablesExtension.create());
		this.parser = Parser.builder().extensions(extensions).build();
		this.renderer = HtmlRenderer.builder().extensions(extensions).build();
	}

	/**
	 * Convert Markdown to a complete, styled HTML document suitable for PDF
	 * conversion via iText html2pdf.
	 */
	public String renderMarkdownToHtml(String markdown) {
		Node document = parser.parse(markdown);
		String body = renderer.render(document);
		return wrapHtml(body);
	}

	/**
	 * Parse Markdown into a CommonMark AST node. Used by services that need the
	 * AST for further processing (e.g. DOCX generation via Apache POI).
	 */
	public Node parseToNode(String markdown) {
		return parser.parse(markdown);
	}

	private String wrapHtml(String body) {
		return """
				<!DOCTYPE html>
				<html>
				<head>
				<meta charset="UTF-8"/>
				<style>
				@page { size: A4 landscape; margin: 30pt; }
				""" + HTML_STYLES + """
				</style>
				</head>
				<body>
				""" + body + """
				</body>
				</html>
				""";
	}
}
