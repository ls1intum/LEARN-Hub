package com.learnhub.documentmanagement.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.*;
import org.commonmark.ext.gfm.tables.*;
import org.commonmark.node.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Service for rendering Markdown content to DOCX (Word) format using Apache
 * POI. Uses {@link MarkdownToHtmlService} for shared Markdown parsing via its
 * {@link MarkdownToHtmlService#parseToNode(String)} method.
 */
@Service
public class MarkdownToDocxService {

	private static final Logger logger = LoggerFactory.getLogger(MarkdownToDocxService.class);
	private static final String DOCX_TEMPLATE_PATH = "templates/markdown/docx-template.properties";
	private static final String LOGO_PATH = "templates/markdown/header-logo.png";
	private static final DateTimeFormatter FOOTER_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

	private final MarkdownToHtmlService markdownToHtmlService;
	private final DocxTemplateSettings templateSettings;

	public MarkdownToDocxService(MarkdownToHtmlService markdownToHtmlService) {
		this.markdownToHtmlService = markdownToHtmlService;
		this.templateSettings = DocxTemplateSettings.load(DOCX_TEMPLATE_PATH);
	}

	/**
	 * Render markdown content to DOCX bytes (default landscape).
	 */
	public byte[] renderMarkdownToDocx(String markdown) {
		return renderMarkdownToDocx(markdown, true, "");
	}

	/**
	 * Render markdown content to DOCX bytes with specified orientation.
	 *
	 * @param markdown
	 *            the markdown content
	 * @param landscape
	 *            true for landscape, false for portrait
	 */
	public byte[] renderMarkdownToDocx(String markdown, boolean landscape) {
		return renderMarkdownToDocx(markdown, landscape, "");
	}

	/**
	 * Render markdown content to DOCX bytes with specified orientation and activity
	 * name in the header.
	 *
	 * @param markdown
	 *            the markdown content
	 * @param landscape
	 *            true for landscape, false for portrait
	 * @param activityName
	 *            the activity name shown in the page header
	 */
	public byte[] renderMarkdownToDocx(String markdown, boolean landscape, String activityName) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); XWPFDocument document = new XWPFDocument()) {

			CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
			CTPageSz pageSize = sectPr.addNewPgSz();
			if (landscape) {
				pageSize.setW(BigInteger.valueOf(templateSettings.pageWidthTwips()));
				pageSize.setH(BigInteger.valueOf(templateSettings.pageHeightTwips()));
				pageSize.setOrient(STPageOrientation.LANDSCAPE);
			} else {
				// Portrait: swap width/height compared to landscape
				pageSize.setW(BigInteger.valueOf(templateSettings.pageHeightTwips()));
				pageSize.setH(BigInteger.valueOf(templateSettings.pageWidthTwips()));
				pageSize.setOrient(STPageOrientation.PORTRAIT);
			}

			configureHeaderAndFooter(document, sectPr, activityName);

			Node docNode = markdownToHtmlService.parseToNode(markdown);
			renderNode(document, docNode);

			document.write(baos);
			return baos.toByteArray();
		} catch (Exception e) {
			logger.error("Failed to render markdown to DOCX: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to render markdown to DOCX: " + e.getMessage(), e);
		}
	}

	private void configureHeaderAndFooter(XWPFDocument document, CTSectPr sectPr, String activityName)
			throws IOException {
		XWPFHeaderFooterPolicy headerFooterPolicy = new XWPFHeaderFooterPolicy(document, sectPr);
		XWPFHeader header = headerFooterPolicy.createHeader(XWPFHeaderFooterPolicy.DEFAULT);
		XWPFFooter footer = headerFooterPolicy.createFooter(XWPFHeaderFooterPolicy.DEFAULT);

		configureHeader(header, activityName);
		configureFooter(footer);
	}

	private void configureHeader(XWPFHeader header, String activityName) throws IOException {
		XWPFTable table = header.createTable(1, 1);
		configureBorderlessTable(table, new int[]{10000});

		XWPFTableCell cell = table.getRow(0).getCell(0);
		setCellWidth(cell, 10000);
		setCellVerticalAlignment(cell, STVerticalJc.CENTER);
		setHeaderContent(cell, activityName);
	}

	private void configureFooter(XWPFFooter footer) {
		XWPFTable table = footer.createTable(1, 3);
		configureBorderlessTable(table, new int[]{3200, 3600, 3200});

		XWPFTableCell leftCell = table.getRow(0).getCell(0);
		setCellWidth(leftCell, 3200);
		setFooterDate(leftCell);

		XWPFTableCell centerCell = table.getRow(0).getCell(1);
		setCellWidth(centerCell, 3600);
		setFooterCenter(centerCell);

		XWPFTableCell rightCell = table.getRow(0).getCell(2);
		setCellWidth(rightCell, 3200);
		setFooterPageNumber(rightCell);
	}

	private void configureBorderlessTable(XWPFTable table, int[] widths) {
		CTTblPr tableProperties = table.getCTTbl().getTblPr();
		if (tableProperties == null) {
			tableProperties = table.getCTTbl().addNewTblPr();
		}

		CTTblWidth tableWidth = tableProperties.isSetTblW() ? tableProperties.getTblW() : tableProperties.addNewTblW();
		tableWidth.setType(STTblWidth.PCT);
		tableWidth.setW(BigInteger.valueOf(5000));

		CTTblBorders borders = tableProperties.isSetTblBorders()
				? tableProperties.getTblBorders()
				: tableProperties.addNewTblBorders();
		setNilBorder(borders.addNewTop());
		setNilBorder(borders.addNewBottom());
		setNilBorder(borders.addNewLeft());
		setNilBorder(borders.addNewRight());
		setNilBorder(borders.addNewInsideH());
		setNilBorder(borders.addNewInsideV());

		for (int i = 0; i < widths.length; i++) {
			setCellWidth(table.getRow(0).getCell(i), widths[i]);
		}
	}

	private void setNilBorder(CTBorder border) {
		border.setVal(STBorder.NIL);
	}

	private void setCellWidth(XWPFTableCell cell, int widthTwips) {
		CTTcPr cellProperties = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
		CTTblWidth width = cellProperties.isSetTcW() ? cellProperties.getTcW() : cellProperties.addNewTcW();
		width.setType(STTblWidth.DXA);
		width.setW(BigInteger.valueOf(widthTwips));
	}

	private void setHeaderContent(XWPFTableCell cell, String activityName) throws IOException {
		clearCell(cell);
		XWPFParagraph paragraph = cell.addParagraph();
		paragraph.setAlignment(ParagraphAlignment.RIGHT);

		XWPFRun textRun = paragraph.createRun();
		textRun.setText(activityName != null ? activityName : "");
		textRun.setFontSize(10);
		textRun.setColor("555555");

		try (InputStream inputStream = new ClassPathResource(LOGO_PATH).getInputStream()) {
			// Add spacing between activity name and logo
			XWPFRun spacerRun = paragraph.createRun();
			spacerRun.setText("  ");
			XWPFRun imageRun = paragraph.createRun();
			imageRun.addPicture(inputStream, org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_PNG, "header-logo.png",
					Units.toEMU(110), Units.toEMU(24));
		} catch (Exception e) {
			throw new IOException("Failed to add DOCX header logo", e);
		}
	}

	private void setCellVerticalAlignment(XWPFTableCell cell, STVerticalJc.Enum alignment) {
		CTTcPr cellProperties = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
		CTVerticalJc vAlign = cellProperties.isSetVAlign() ? cellProperties.getVAlign() : cellProperties.addNewVAlign();
		vAlign.setVal(alignment);
	}

	private void setFooterDate(XWPFTableCell cell) {
		clearCell(cell);
		XWPFParagraph paragraph = cell.addParagraph();
		paragraph.setAlignment(ParagraphAlignment.LEFT);

		XWPFRun run = paragraph.createRun();
		run.setText(LocalDateTime.now().format(FOOTER_DATE_FORMATTER));
		run.setFontSize(7);
		run.setColor("555555");
	}

	private void setFooterCenter(XWPFTableCell cell) {
		clearCell(cell);

		XWPFParagraph paragraph = cell.addParagraph();
		paragraph.setAlignment(ParagraphAlignment.CENTER);
		XWPFRun run = paragraph.createRun();
		run.setText("LEARN-Hub \u2013 a TUM Applied Education Technologies application \u00B7 aet.cit.tum.de");
		run.setFontSize(7);
		run.setColor("555555");
	}

	private void setFooterPageNumber(XWPFTableCell cell) {
		clearCell(cell);
		XWPFParagraph paragraph = cell.addParagraph();
		paragraph.setAlignment(ParagraphAlignment.RIGHT);

		XWPFRun labelRun = paragraph.createRun();
		labelRun.setText("Page ");
		labelRun.setFontSize(7);
		labelRun.setColor("555555");

		CTSimpleField pageField = paragraph.getCTP().addNewFldSimple();
		pageField.setInstr("PAGE");
		CTR pageRun = pageField.addNewR();
		CTRPr pageRunProps = pageRun.addNewRPr();
		pageRunProps.addNewSz().setVal(BigInteger.valueOf(14));
		pageRunProps.addNewColor().setVal("555555");
		CTText pageText = pageRun.addNewT();
		pageText.setStringValue("1");

		XWPFRun ofRun = paragraph.createRun();
		ofRun.setText(" of ");
		ofRun.setFontSize(7);
		ofRun.setColor("555555");

		CTSimpleField totalPagesField = paragraph.getCTP().addNewFldSimple();
		totalPagesField.setInstr("NUMPAGES");
		CTR totalPagesRun = totalPagesField.addNewR();
		CTRPr totalPagesRunProps = totalPagesRun.addNewRPr();
		totalPagesRunProps.addNewSz().setVal(BigInteger.valueOf(14));
		totalPagesRunProps.addNewColor().setVal("555555");
		CTText totalPagesText = totalPagesRun.addNewT();
		totalPagesText.setStringValue("1");
	}

	private void clearCell(XWPFTableCell cell) {
		for (int i = cell.getParagraphs().size() - 1; i >= 0; i--) {
			cell.removeParagraph(i);
		}
	}

	private void renderNode(XWPFDocument document, Node node) {
		Node child = node.getFirstChild();
		while (child != null) {
			if (child instanceof Heading heading) {
				renderHeading(document, heading);
			} else if (child instanceof org.commonmark.node.Paragraph para) {
				renderParagraph(document, para);
			} else if (child instanceof BulletList bulletList) {
				renderBulletList(document, bulletList);
			} else if (child instanceof OrderedList orderedList) {
				renderOrderedList(document, orderedList);
			} else if (child instanceof FencedCodeBlock codeBlock) {
				renderCodeBlock(document, codeBlock.getLiteral());
			} else if (child instanceof IndentedCodeBlock codeBlock) {
				renderCodeBlock(document, codeBlock.getLiteral());
			} else if (child instanceof BlockQuote blockQuote) {
				renderBlockQuote(document, blockQuote);
			} else if (child instanceof ThematicBreak) {
				renderThematicBreak(document);
			} else if (child instanceof TableBlock tableBlock) {
				renderTable(document, tableBlock);
			}
			child = child.getNext();
		}
	}

	private void renderHeading(XWPFDocument document, Heading heading) {
		XWPFParagraph para = document.createParagraph();
		int level = heading.getLevel();
		switch (level) {
			case 1 :
				para.setStyle(templateSettings.heading1Style());
				break;
			case 2 :
				para.setStyle(templateSettings.heading2Style());
				break;
			case 3 :
				para.setStyle(templateSettings.heading3Style());
				break;
			default :
				para.setStyle(templateSettings.defaultHeadingStyle());
				break;
		}
		renderInlineContent(para, heading);
		// Apply font size and color for heading
		for (XWPFRun run : para.getRuns()) {
			run.setBold(true);
			run.setColor(templateSettings.headingColor());
			switch (level) {
				case 1 :
					run.setFontSize(templateSettings.heading1FontSize());
					break;
				case 2 :
					run.setFontSize(templateSettings.heading2FontSize());
					break;
				case 3 :
					run.setFontSize(templateSettings.heading3FontSize());
					break;
				default :
					run.setFontSize(templateSettings.defaultHeadingFontSize());
					break;
			}
		}
	}

	private void renderParagraph(XWPFDocument document, org.commonmark.node.Paragraph para) {
		XWPFParagraph xwpfPara = document.createParagraph();
		renderInlineContent(xwpfPara, para);
	}

	private void renderBulletList(XWPFDocument document, BulletList bulletList) {
		Node item = bulletList.getFirstChild();
		while (item != null) {
			if (item instanceof ListItem listItem) {
				renderListItem(document, listItem, "• ");
			}
			item = item.getNext();
		}
	}

	private void renderOrderedList(XWPFDocument document, OrderedList orderedList) {
		Node item = orderedList.getFirstChild();
		int index = orderedList.getMarkerStartNumber();
		while (item != null) {
			if (item instanceof ListItem listItem) {
				renderListItem(document, listItem, index + ". ");
				index++;
			}
			item = item.getNext();
		}
	}

	private void renderListItem(XWPFDocument document, ListItem listItem, String marker) {
		Node child = listItem.getFirstChild();
		boolean first = true;
		while (child != null) {
			if (child instanceof org.commonmark.node.Paragraph para) {
				XWPFParagraph xwpfPara = document.createParagraph();
				xwpfPara.setIndentationLeft(templateSettings.listIndentLeft());
				if (first) {
					XWPFRun markerRun = xwpfPara.createRun();
					markerRun.setText(marker);
					first = false;
				}
				renderInlineContent(xwpfPara, para);
			} else if (child instanceof BulletList nested) {
				renderBulletList(document, nested);
			} else if (child instanceof OrderedList nested) {
				renderOrderedList(document, nested);
			}
			child = child.getNext();
		}
	}

	private void renderCodeBlock(XWPFDocument document, String code) {
		XWPFParagraph para = document.createParagraph();
		para.setIndentationLeft(templateSettings.codeBlockIndentLeft());
		XWPFRun run = para.createRun();
		run.setFontFamily(templateSettings.codeFontFamily());
		run.setFontSize(templateSettings.codeFontSize());
		// Handle multi-line code blocks
		String[] lines = code.split("\\n");
		for (int i = 0; i < lines.length; i++) {
			if (i > 0) {
				run.addBreak();
			}
			run.setText(lines[i]);
		}
	}

	private void renderBlockQuote(XWPFDocument document, BlockQuote blockQuote) {
		Node child = blockQuote.getFirstChild();
		while (child != null) {
			if (child instanceof org.commonmark.node.Paragraph para) {
				XWPFParagraph xwpfPara = document.createParagraph();
				xwpfPara.setIndentationLeft(templateSettings.blockQuoteIndentLeft());
				xwpfPara.setBorderLeft(templateSettings.blockQuoteBorderLeft());
				renderInlineContent(xwpfPara, para);
				// Italicize block quote text
				for (XWPFRun run : xwpfPara.getRuns()) {
					run.setItalic(true);
				}
			}
			child = child.getNext();
		}
	}

	private void renderThematicBreak(XWPFDocument document) {
		XWPFParagraph para = document.createParagraph();
		para.setBorderBottom(templateSettings.thematicBreakBorderBottom());
	}

	private void renderTable(XWPFDocument document, TableBlock tableBlock) {
		// Count columns from header
		TableHead head = null;
		TableBody body = null;
		Node child = tableBlock.getFirstChild();
		while (child != null) {
			if (child instanceof TableHead h) {
				head = h;
			}
			if (child instanceof TableBody b) {
				body = b;
			}
			child = child.getNext();
		}

		if (head == null) {
			return;
		}

		// Count columns
		int numCols = 0;
		TableRow headerRow = (TableRow) head.getFirstChild();
		if (headerRow != null) {
			Node cell = headerRow.getFirstChild();
			while (cell != null) {
				numCols++;
				cell = cell.getNext();
			}
		}

		if (numCols == 0) {
			return;
		}

		XWPFTable table = document.createTable();
		// Remove default empty row
		if (table.getNumberOfRows() > 0) {
			table.removeRow(0);
		}

		// Set table width to 100%
		CTTblPr tblPr = table.getCTTbl().getTblPr();
		if (tblPr == null) {
			tblPr = table.getCTTbl().addNewTblPr();
		}
		CTTblWidth tblWidth = tblPr.addNewTblW();
		tblWidth.setW(BigInteger.valueOf(templateSettings.tableWidthPct()));
		tblWidth.setType(STTblWidth.PCT);

		// Render header row
		if (headerRow != null) {
			XWPFTableRow xwpfRow = table.createRow();
			Node cell = headerRow.getFirstChild();
			int colIdx = 0;
			while (cell != null) {
				if (cell instanceof TableCell tableCell) {
					XWPFTableCell xwpfCell = colIdx < xwpfRow.getTableCells().size()
							? xwpfRow.getCell(colIdx)
							: xwpfRow.addNewTableCell();
					setCellText(xwpfCell, tableCell, true);
					// Dark blue background for header
					CTTcPr tcPr = xwpfCell.getCTTc().addNewTcPr();
					CTShd shd = tcPr.addNewShd();
					shd.setFill(templateSettings.tableHeaderBackground());
					shd.setVal(STShd.CLEAR);
					colIdx++;
				}
				cell = cell.getNext();
			}
		}

		// Render body rows
		if (body != null) {
			Node row = body.getFirstChild();
			int rowIdx = 0;
			while (row != null) {
				if (row instanceof TableRow tableRow) {
					XWPFTableRow xwpfRow = table.createRow();
					Node cell = tableRow.getFirstChild();
					int colIdx = 0;
					while (cell != null) {
						if (cell instanceof TableCell tableCell) {
							XWPFTableCell xwpfCell = colIdx < xwpfRow.getTableCells().size()
									? xwpfRow.getCell(colIdx)
									: xwpfRow.addNewTableCell();
							setCellText(xwpfCell, tableCell, false);
							// Alternating row colors
							if (rowIdx % 2 == 1) {
								CTTcPr tcPr = xwpfCell.getCTTc().addNewTcPr();
								CTShd shd = tcPr.addNewShd();
								shd.setFill(templateSettings.tableBodyAlternateBackground());
								shd.setVal(STShd.CLEAR);
							}
							colIdx++;
						}
						cell = cell.getNext();
					}
					rowIdx++;
				}
				row = row.getNext();
			}
		}

		// Add spacing after table
		document.createParagraph();
	}

	private void setCellText(XWPFTableCell xwpfCell, TableCell tableCell, boolean isHeader) {
		// Clear existing paragraphs
		for (int i = xwpfCell.getParagraphs().size() - 1; i >= 0; i--) {
			xwpfCell.removeParagraph(i);
		}
		XWPFParagraph para = xwpfCell.addParagraph();
		// Render inline content from the cell's child nodes
		Node child = tableCell.getFirstChild();
		while (child != null) {
			if (child instanceof org.commonmark.node.Paragraph p) {
				renderInlineContent(para, p);
			} else if (child instanceof Text text) {
				XWPFRun run = para.createRun();
				run.setText(text.getLiteral());
			}
			child = child.getNext();
		}
		// Style the runs
		for (XWPFRun run : para.getRuns()) {
			run.setFontSize(templateSettings.tableBodyFontSize());
			if (isHeader) {
				run.setBold(true);
				run.setColor(templateSettings.tableHeaderFontColor());
				run.setFontSize(templateSettings.tableHeaderFontSize());
			}
		}
	}

	private void renderInlineContent(XWPFParagraph para, Node parent) {
		Node child = parent.getFirstChild();
		while (child != null) {
			renderInline(para, child, false, false);
			child = child.getNext();
		}
	}

	private void renderInline(XWPFParagraph para, Node node, boolean bold, boolean italic) {
		if (node instanceof Text text) {
			XWPFRun run = para.createRun();
			run.setText(text.getLiteral());
			if (bold) {
				run.setBold(true);
			}
			if (italic) {
				run.setItalic(true);
			}
		} else if (node instanceof StrongEmphasis) {
			Node child = node.getFirstChild();
			while (child != null) {
				renderInline(para, child, true, italic);
				child = child.getNext();
			}
		} else if (node instanceof Emphasis) {
			Node child = node.getFirstChild();
			while (child != null) {
				renderInline(para, child, bold, true);
				child = child.getNext();
			}
		} else if (node instanceof Code code) {
			XWPFRun run = para.createRun();
			run.setText(code.getLiteral());
			run.setFontFamily(templateSettings.codeFontFamily());
		} else if (node instanceof SoftLineBreak) {
			XWPFRun run = para.createRun();
			run.setText(" ");
		} else if (node instanceof HardLineBreak) {
			XWPFRun run = para.createRun();
			run.addBreak();
		} else if (node instanceof Link link) {
			XWPFRun run = para.createRun();
			run.setText(extractText(link));
			run.setUnderline(templateSettings.linkUnderline());
			run.setColor(templateSettings.linkColor());
		} else {
			// For any other inline node, try to render its children
			Node child = node.getFirstChild();
			while (child != null) {
				renderInline(para, child, bold, italic);
				child = child.getNext();
			}
		}
	}

	private String extractText(Node node) {
		StringBuilder sb = new StringBuilder();
		Node child = node.getFirstChild();
		while (child != null) {
			if (child instanceof Text text) {
				sb.append(text.getLiteral());
			} else {
				sb.append(extractText(child));
			}
			child = child.getNext();
		}
		return sb.toString();
	}

	record DocxTemplateSettings(int pageWidthTwips, int pageHeightTwips, STPageOrientation.Enum pageOrientation,
			String heading1Style, int heading1FontSize, String heading2Style, int heading2FontSize,
			String heading3Style, int heading3FontSize, String defaultHeadingStyle, int defaultHeadingFontSize,
			String headingColor, int listIndentLeft, int codeBlockIndentLeft, String codeFontFamily, int codeFontSize,
			int blockQuoteIndentLeft, Borders blockQuoteBorderLeft, Borders thematicBreakBorderBottom,
			int tableWidthPct, String tableHeaderBackground, String tableHeaderFontColor, int tableHeaderFontSize,
			int tableBodyFontSize, String tableBodyAlternateBackground, String linkColor,
			UnderlinePatterns linkUnderline) {

		static DocxTemplateSettings load(String path) {
			Properties properties = new Properties();
			ClassPathResource resource = new ClassPathResource(path);
			try (InputStream inputStream = resource.getInputStream()) {
				properties.load(new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new IllegalStateException("Failed to load DOCX template settings: " + path, e);
			}

			return new DocxTemplateSettings(getInt(properties, "page.width.twips"),
					getInt(properties, "page.height.twips"),
					STPageOrientation.Enum.forString(getString(properties, "page.orientation")),
					getString(properties, "heading.1.style"), getInt(properties, "heading.1.fontSize"),
					getString(properties, "heading.2.style"), getInt(properties, "heading.2.fontSize"),
					getString(properties, "heading.3.style"), getInt(properties, "heading.3.fontSize"),
					getString(properties, "heading.default.style"), getInt(properties, "heading.default.fontSize"),
					getString(properties, "heading.color"), getInt(properties, "list.indent.left"),
					getInt(properties, "code.block.indent.left"), getString(properties, "code.font.family"),
					getInt(properties, "code.font.size"), getInt(properties, "blockquote.indent.left"),
					Borders.valueOf(getString(properties, "blockquote.border.left")),
					Borders.valueOf(getString(properties, "thematic.break.border.bottom")),
					getInt(properties, "table.width.pct"), getString(properties, "table.header.background"),
					getString(properties, "table.header.font.color"), getInt(properties, "table.header.font.size"),
					getInt(properties, "table.body.font.size"),
					getString(properties, "table.body.alternate.background"), getString(properties, "link.color"),
					UnderlinePatterns.valueOf(getString(properties, "link.underline")));
		}

		private static int getInt(Properties properties, String key) {
			return Integer.parseInt(getString(properties, key));
		}

		private static String getString(Properties properties, String key) {
			String value = properties.getProperty(key);
			if (value == null || value.isBlank()) {
				throw new IllegalStateException("Missing DOCX template setting: " + key);
			}
			return value.trim();
		}
	}
}
