package com.learnhub.documentmanagement.service;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.List;
import org.apache.poi.xwpf.usermodel.*;
import org.commonmark.ext.gfm.tables.*;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for rendering Markdown content to DOCX (Word) format using Apache POI
 * and commonmark for markdown parsing.
 */
@Service
public class MarkdownToDocxService {

	private static final Logger logger = LoggerFactory.getLogger(MarkdownToDocxService.class);

	private final Parser parser;

	public MarkdownToDocxService() {
		this.parser = Parser.builder().extensions(List.of(TablesExtension.create())).build();
	}

	/**
	 * Render markdown content to DOCX bytes.
	 */
	public byte[] renderMarkdownToDocx(String markdown) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); XWPFDocument document = new XWPFDocument()) {

			// Set default page to landscape
			CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
			CTPageSz pageSize = sectPr.addNewPgSz();
			// A4 landscape: width=16838 (297mm), height=11906 (210mm) in twips
			pageSize.setW(BigInteger.valueOf(16838));
			pageSize.setH(BigInteger.valueOf(11906));
			pageSize.setOrient(STPageOrientation.LANDSCAPE);

			Node docNode = parser.parse(markdown);
			renderNode(document, docNode);

			document.write(baos);
			return baos.toByteArray();
		} catch (Exception e) {
			logger.error("Failed to render markdown to DOCX: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to render markdown to DOCX: " + e.getMessage(), e);
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
				para.setStyle("Heading1");
				break;
			case 2 :
				para.setStyle("Heading2");
				break;
			case 3 :
				para.setStyle("Heading3");
				break;
			default :
				para.setStyle("Heading4");
				break;
		}
		renderInlineContent(para, heading);
		// Apply font size for heading
		for (XWPFRun run : para.getRuns()) {
			run.setBold(true);
			switch (level) {
				case 1 :
					run.setFontSize(24);
					break;
				case 2 :
					run.setFontSize(18);
					break;
				case 3 :
					run.setFontSize(14);
					break;
				default :
					run.setFontSize(12);
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
				xwpfPara.setIndentationLeft(720); // 0.5 inch indent
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
		para.setIndentationLeft(360);
		XWPFRun run = para.createRun();
		run.setFontFamily("Courier New");
		run.setFontSize(9);
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
				xwpfPara.setIndentationLeft(720);
				xwpfPara.setBorderLeft(Borders.SINGLE);
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
		para.setBorderBottom(Borders.SINGLE);
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
		tblWidth.setW(BigInteger.valueOf(5000));
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
					shd.setFill("29417A");
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
								shd.setFill("F0F4FA");
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
			run.setFontSize(9);
			if (isHeader) {
				run.setBold(true);
				run.setColor("FFFFFF");
				run.setFontSize(10);
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
			run.setFontFamily("Courier New");
		} else if (node instanceof SoftLineBreak) {
			XWPFRun run = para.createRun();
			run.setText(" ");
		} else if (node instanceof HardLineBreak) {
			XWPFRun run = para.createRun();
			run.addBreak();
		} else if (node instanceof Link link) {
			XWPFRun run = para.createRun();
			run.setText(extractText(link));
			run.setUnderline(UnderlinePatterns.SINGLE);
			run.setColor("0563C1");
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
}
