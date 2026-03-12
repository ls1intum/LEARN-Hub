package com.learnhub.documentmanagement.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for rendering Artikulationsschema markdown into a styled PDF using
 * iText7.
 */
@Service
public class ArtikulationsschemaService {

	private static final Logger logger = LoggerFactory.getLogger(ArtikulationsschemaService.class);

	private static final DeviceRgb HEADER_BG_COLOR = new DeviceRgb(41, 65, 122);
	private static final DeviceRgb ALT_ROW_COLOR = new DeviceRgb(240, 244, 250);
	private static final float FONT_SIZE_TITLE = 18f;
	private static final float FONT_SIZE_META = 11f;
	private static final float FONT_SIZE_TABLE = 9f;
	private static final float FONT_SIZE_TABLE_HEADER = 10f;

	/**
	 * Render Artikulationsschema markdown to PDF bytes.
	 */
	public byte[] renderMarkdownToPdf(String markdown) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			PdfWriter writer = new PdfWriter(baos);
			PdfDocument pdfDoc = new PdfDocument(writer);
			pdfDoc.setDefaultPageSize(PageSize.A4.rotate());
			Document document = new Document(pdfDoc, PageSize.A4.rotate(), false);
			document.setMargins(30, 30, 30, 30);

			PdfFont regular = PdfFontFactory.createFont("Helvetica");
			PdfFont bold = PdfFontFactory.createFont("Helvetica-Bold");

			// Parse markdown
			ParsedSchema schema = parseMarkdown(markdown);

			// Title
			document.add(new Paragraph(schema.title).setFont(bold).setFontSize(FONT_SIZE_TITLE)
					.setTextAlignment(TextAlignment.CENTER).setMarginBottom(10));

			// Metadata fields
			for (String meta : schema.metaLines) {
				String[] parts = meta.split(":", 2);
				if (parts.length == 2) {
					Paragraph p = new Paragraph().setFontSize(FONT_SIZE_META).setMarginBottom(2);
					p.add(new com.itextpdf.layout.element.Text(parts[0].trim() + ": ").setFont(bold));
					p.add(new com.itextpdf.layout.element.Text(parts[1].trim()).setFont(regular));
					document.add(p);
				} else {
					document.add(new Paragraph(meta).setFont(regular).setFontSize(FONT_SIZE_META).setMarginBottom(2));
				}
			}

			document.add(new Paragraph("").setMarginBottom(10));

			// Table
			if (!schema.headers.isEmpty() && !schema.rows.isEmpty()) {
				int numCols = schema.headers.size();
				float[] colWidths = getColumnWidths(numCols);
				Table table = new Table(UnitValue.createPercentArray(colWidths)).useAllAvailableWidth();

				// Header row
				for (String header : schema.headers) {
					Cell cell = new Cell().add(new Paragraph(header).setFont(bold).setFontSize(FONT_SIZE_TABLE_HEADER)
							.setFontColor(ColorConstants.WHITE));
					cell.setBackgroundColor(HEADER_BG_COLOR);
					cell.setPadding(6);
					cell.setBorder(new SolidBorder(ColorConstants.WHITE, 0.5f));
					table.addHeaderCell(cell);
				}

				// Data rows
				for (int rowIdx = 0; rowIdx < schema.rows.size(); rowIdx++) {
					List<String> row = schema.rows.get(rowIdx);
					boolean altRow = rowIdx % 2 == 1;
					for (int colIdx = 0; colIdx < numCols; colIdx++) {
						String cellText = colIdx < row.size() ? row.get(colIdx) : "";
						Cell cell = new Cell()
								.add(new Paragraph(cellText).setFont(regular).setFontSize(FONT_SIZE_TABLE));
						cell.setPadding(5);
						cell.setBorder(new SolidBorder(new DeviceRgb(200, 200, 200), 0.5f));
						if (altRow) {
							cell.setBackgroundColor(ALT_ROW_COLOR);
						}
						table.addCell(cell);
					}
				}

				document.add(table);
			}

			document.close();
			return baos.toByteArray();
		} catch (Exception e) {
			logger.error("Failed to render Artikulationsschema PDF: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to render Artikulationsschema PDF: " + e.getMessage(), e);
		}
	}

	private float[] getColumnWidths(int numCols) {
		if (numCols == 6) {
			// Standard schema: Zeit, Phase, Handlungsschritte, Sozialform, Kompetenzen,
			// Medien/Material
			return new float[]{8f, 12f, 30f, 12f, 20f, 18f};
		}
		float[] widths = new float[numCols];
		float equal = 100f / numCols;
		for (int i = 0; i < numCols; i++) {
			widths[i] = equal;
		}
		return widths;
	}

	// --- Markdown parsing ---

	static class ParsedSchema {

		String title = "Artikulationsschema";
		List<String> metaLines = new ArrayList<>();
		List<String> headers = new ArrayList<>();
		List<List<String>> rows = new ArrayList<>();
	}

	private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
	private static final Pattern TABLE_ROW_PATTERN = Pattern.compile("^\\|(.+)\\|\\s*$");
	private static final Pattern SEPARATOR_PATTERN = Pattern.compile("^\\|[\\s\\-:|]+\\|\\s*$");

	ParsedSchema parseMarkdown(String markdown) {
		ParsedSchema schema = new ParsedSchema();
		String[] lines = markdown.split("\\n");
		boolean headerParsed = false;

		for (String line : lines) {
			String trimmed = line.trim();

			// Title (# heading)
			if (trimmed.startsWith("# ")) {
				schema.title = trimmed.substring(2).trim();
				continue;
			}

			// Skip sub-headings
			if (trimmed.startsWith("## ") || trimmed.startsWith("### ")) {
				continue;
			}

			// Metadata lines (**Key:** Value)
			Matcher boldMatcher = BOLD_PATTERN.matcher(trimmed);
			if (boldMatcher.find() && !trimmed.startsWith("|")) {
				String cleaned = trimmed.replaceAll("\\*\\*", "");
				schema.metaLines.add(cleaned);
				continue;
			}

			// Table separator line
			if (SEPARATOR_PATTERN.matcher(trimmed).matches()) {
				continue;
			}

			// Table row
			Matcher rowMatcher = TABLE_ROW_PATTERN.matcher(trimmed);
			if (rowMatcher.matches()) {
				String rowContent = rowMatcher.group(1);
				List<String> cells = parseCells(rowContent);
				if (!headerParsed) {
					schema.headers = cells;
					headerParsed = true;
				} else {
					schema.rows.add(cells);
				}
			}
		}

		return schema;
	}

	private List<String> parseCells(String rowContent) {
		String[] parts = rowContent.split("\\|", -1);
		List<String> cells = new ArrayList<>();
		boolean foundContent = false;
		for (String part : parts) {
			String trimmed = part.trim();
			if (!foundContent && trimmed.isEmpty()) {
				// Skip leading empty cells from the split
				continue;
			}
			foundContent = true;
			cells.add(trimmed);
		}
		// Remove trailing empty cell if present
		while (!cells.isEmpty() && cells.get(cells.size() - 1).isEmpty()) {
			cells.remove(cells.size() - 1);
		}
		return cells;
	}
}
