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
import com.itextpdf.layout.element.List;
import com.itextpdf.layout.element.ListItem;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import java.io.ByteArrayOutputStream;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for rendering markdown content into a styled PDF using
 * iText7 and commonmark for generic markdown parsing. Supports headings,
 * paragraphs, bold/italic text, lists, tables, code blocks, block quotes, and
 * more.
 */
@Service
public class MarkdownToPdfService {

	private static final Logger logger = LoggerFactory.getLogger(MarkdownToPdfService.class);

	private static final DeviceRgb HEADER_BG_COLOR = new DeviceRgb(41, 65, 122);
	private static final DeviceRgb ALT_ROW_COLOR = new DeviceRgb(240, 244, 250);
	private static final DeviceRgb BLOCKQUOTE_BORDER = new DeviceRgb(180, 180, 180);
	private static final float FONT_SIZE_TITLE = 18f;
	private static final float FONT_SIZE_H2 = 15f;
	private static final float FONT_SIZE_H3 = 13f;
	private static final float FONT_SIZE_BODY = 11f;
	private static final float FONT_SIZE_TABLE = 9f;
	private static final float FONT_SIZE_TABLE_HEADER = 10f;
	private static final float FONT_SIZE_CODE = 9f;

	private final Parser parser;

	public MarkdownToPdfService() {
		this.parser = Parser.builder().extensions(java.util.List.of(TablesExtension.create())).build();
	}

	/**
	 * Render markdown content to PDF bytes.
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
			PdfFont italic = PdfFontFactory.createFont("Helvetica-Oblique");
			PdfFont boldItalic = PdfFontFactory.createFont("Helvetica-BoldOblique");
			PdfFont mono = PdfFontFactory.createFont("Courier");

			FontSet fonts = new FontSet(regular, bold, italic, boldItalic, mono);

			Node docNode = parser.parse(markdown);
			renderNode(document, docNode, fonts);

			document.close();
			return baos.toByteArray();
		} catch (Exception e) {
			logger.error("Failed to render markdown PDF: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to render markdown PDF: " + e.getMessage(), e);
		}
	}

	/**
	 * Container for font variants used during rendering.
	 */
	record FontSet(PdfFont regular, PdfFont bold, PdfFont italic, PdfFont boldItalic, PdfFont mono) {
	}

	private void renderNode(Document document, Node node, FontSet fonts) {
		Node child = node.getFirstChild();
		while (child != null) {
			if (child instanceof Heading heading) {
				renderHeading(document, heading, fonts);
			} else if (child instanceof org.commonmark.node.Paragraph para) {
				renderParagraph(document, para, fonts);
			} else if (child instanceof BulletList bulletList) {
				renderBulletList(document, bulletList, fonts);
			} else if (child instanceof OrderedList orderedList) {
				renderOrderedList(document, orderedList, fonts);
			} else if (child instanceof FencedCodeBlock codeBlock) {
				renderCodeBlock(document, codeBlock.getLiteral(), fonts);
			} else if (child instanceof IndentedCodeBlock codeBlock) {
				renderCodeBlock(document, codeBlock.getLiteral(), fonts);
			} else if (child instanceof BlockQuote blockQuote) {
				renderBlockQuote(document, blockQuote, fonts);
			} else if (child instanceof ThematicBreak) {
				document.add(new Paragraph("").setBorderBottom(new SolidBorder(0.5f)).setMarginBottom(10));
			} else if (child instanceof TableBlock tableBlock) {
				renderTable(document, tableBlock, fonts);
			}
			child = child.getNext();
		}
	}

	private void renderHeading(Document document, Heading heading, FontSet fonts) {
		Paragraph p = new Paragraph();
		p.setFont(fonts.bold());
		switch (heading.getLevel()) {
			case 1 :
				p.setFontSize(FONT_SIZE_TITLE).setTextAlignment(TextAlignment.CENTER).setMarginBottom(10);
				break;
			case 2 :
				p.setFontSize(FONT_SIZE_H2).setMarginTop(12).setMarginBottom(6);
				break;
			case 3 :
				p.setFontSize(FONT_SIZE_H3).setMarginTop(10).setMarginBottom(4);
				break;
			default :
				p.setFontSize(FONT_SIZE_BODY).setMarginTop(8).setMarginBottom(4);
				break;
		}
		addInlineContent(p, heading, fonts, false, false);
		document.add(p);
	}

	private void renderParagraph(Document document, org.commonmark.node.Paragraph para, FontSet fonts) {
		Paragraph p = new Paragraph().setFont(fonts.regular()).setFontSize(FONT_SIZE_BODY).setMarginBottom(6);
		addInlineContent(p, para, fonts, false, false);
		document.add(p);
	}

	private void renderBulletList(Document document, BulletList bulletList, FontSet fonts) {
		List list = new List().setSymbolIndent(12).setListSymbol("• ").setFont(fonts.regular())
				.setFontSize(FONT_SIZE_BODY);
		Node item = bulletList.getFirstChild();
		while (item != null) {
			if (item instanceof org.commonmark.node.ListItem li) {
				ListItem listItem = new ListItem();
				renderListItemContent(listItem, li, fonts);
				list.add(listItem);
			}
			item = item.getNext();
		}
		document.add(list);
	}

	private void renderOrderedList(Document document, OrderedList orderedList, FontSet fonts) {
		List list = new List().setSymbolIndent(12).setFont(fonts.regular()).setFontSize(FONT_SIZE_BODY);
		list.setListSymbol(""); // We'll use counter-based numbering
		Node item = orderedList.getFirstChild();
		int index = orderedList.getMarkerStartNumber();
		while (item != null) {
			if (item instanceof org.commonmark.node.ListItem li) {
				ListItem listItem = new ListItem();
				listItem.setListSymbol(index + ". ");
				renderListItemContent(listItem, li, fonts);
				list.add(listItem);
				index++;
			}
			item = item.getNext();
		}
		document.add(list);
	}

	private void renderListItemContent(ListItem listItem, org.commonmark.node.ListItem li, FontSet fonts) {
		Node child = li.getFirstChild();
		while (child != null) {
			if (child instanceof org.commonmark.node.Paragraph para) {
				Paragraph p = new Paragraph().setFont(fonts.regular()).setFontSize(FONT_SIZE_BODY);
				addInlineContent(p, para, fonts, false, false);
				listItem.add(p);
			}
			child = child.getNext();
		}
	}

	private void renderCodeBlock(Document document, String code, FontSet fonts) {
		if (code == null || code.isEmpty()) {
			return;
		}
		Paragraph p = new Paragraph().setFont(fonts.mono()).setFontSize(FONT_SIZE_CODE)
				.setBackgroundColor(new DeviceRgb(245, 245, 245)).setPadding(8).setMarginBottom(8);
		String[] lines = code.split("\\n");
		for (int i = 0; i < lines.length; i++) {
			if (i > 0) {
				p.add("\n");
			}
			p.add(new Text(lines[i]).setFont(fonts.mono()));
		}
		document.add(p);
	}

	private void renderBlockQuote(Document document, BlockQuote blockQuote, FontSet fonts) {
		Node child = blockQuote.getFirstChild();
		while (child != null) {
			if (child instanceof org.commonmark.node.Paragraph para) {
				Paragraph p = new Paragraph().setFont(fonts.italic()).setFontSize(FONT_SIZE_BODY)
						.setBorderLeft(new SolidBorder(BLOCKQUOTE_BORDER, 2f)).setPaddingLeft(10).setMarginBottom(6)
						.setFontColor(new DeviceRgb(100, 100, 100));
				addInlineContent(p, para, fonts, false, true);
				document.add(p);
			}
			child = child.getNext();
		}
	}

	private void renderTable(Document document, TableBlock tableBlock, FontSet fonts) {
		// Find head and body
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

		// Count columns from header row
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

		float[] colWidths = getColumnWidths(numCols);
		Table table = new Table(UnitValue.createPercentArray(colWidths)).useAllAvailableWidth();

		// Header row
		if (headerRow != null) {
			Node cell = headerRow.getFirstChild();
			while (cell != null) {
				if (cell instanceof TableCell tableCell) {
					Paragraph p = new Paragraph().setFont(fonts.bold()).setFontSize(FONT_SIZE_TABLE_HEADER)
							.setFontColor(ColorConstants.WHITE);
					addTableCellContent(p, tableCell, fonts);
					Cell iCell = new Cell().add(p);
					iCell.setBackgroundColor(HEADER_BG_COLOR);
					iCell.setPadding(6);
					iCell.setBorder(new SolidBorder(ColorConstants.WHITE, 0.5f));
					table.addHeaderCell(iCell);
				}
				cell = cell.getNext();
			}
		}

		// Body rows
		if (body != null) {
			Node row = body.getFirstChild();
			int rowIdx = 0;
			while (row != null) {
				if (row instanceof TableRow tableRow) {
					Node cell = tableRow.getFirstChild();
					while (cell != null) {
						if (cell instanceof TableCell tableCell) {
							Paragraph p = new Paragraph().setFont(fonts.regular()).setFontSize(FONT_SIZE_TABLE);
							addTableCellContent(p, tableCell, fonts);
							Cell iCell = new Cell().add(p);
							iCell.setPadding(5);
							iCell.setBorder(new SolidBorder(new DeviceRgb(200, 200, 200), 0.5f));
							if (rowIdx % 2 == 1) {
								iCell.setBackgroundColor(ALT_ROW_COLOR);
							}
							table.addCell(iCell);
						}
						cell = cell.getNext();
					}
					rowIdx++;
				}
				row = row.getNext();
			}
		}

		document.add(table);
		document.add(new Paragraph("").setMarginBottom(10));
	}

	private void addTableCellContent(Paragraph p, TableCell tableCell, FontSet fonts) {
		Node child = tableCell.getFirstChild();
		while (child != null) {
			if (child instanceof org.commonmark.node.Paragraph para) {
				addInlineContent(p, para, fonts, false, false);
			} else if (child instanceof org.commonmark.node.Text text) {
				p.add(new Text(text.getLiteral()));
			}
			child = child.getNext();
		}
	}

	private void addInlineContent(Paragraph p, Node parent, FontSet fonts, boolean bold, boolean italic) {
		Node child = parent.getFirstChild();
		while (child != null) {
			addInline(p, child, fonts, bold, italic);
			child = child.getNext();
		}
	}

	private void addInline(Paragraph p, Node node, FontSet fonts, boolean bold, boolean italic) {
		if (node instanceof org.commonmark.node.Text text) {
			Text t = new Text(text.getLiteral());
			if (bold && italic) {
				t.setFont(fonts.boldItalic());
			} else if (bold) {
				t.setFont(fonts.bold());
			} else if (italic) {
				t.setFont(fonts.italic());
			} else {
				t.setFont(fonts.regular());
			}
			p.add(t);
		} else if (node instanceof StrongEmphasis) {
			Node child = node.getFirstChild();
			while (child != null) {
				addInline(p, child, fonts, true, italic);
				child = child.getNext();
			}
		} else if (node instanceof Emphasis) {
			Node child = node.getFirstChild();
			while (child != null) {
				addInline(p, child, fonts, bold, true);
				child = child.getNext();
			}
		} else if (node instanceof Code code) {
			Text t = new Text(code.getLiteral());
			t.setFont(fonts.mono()).setFontSize(FONT_SIZE_CODE)
					.setBackgroundColor(new DeviceRgb(240, 240, 240));
			p.add(t);
		} else if (node instanceof SoftLineBreak) {
			p.add(new Text(" "));
		} else if (node instanceof HardLineBreak) {
			p.add(new Text("\n"));
		} else if (node instanceof Link) {
			Text t = new Text(extractText(node));
			t.setFont(fonts.regular()).setFontColor(new DeviceRgb(5, 99, 193)).setUnderline();
			p.add(t);
		} else {
			// For any other inline node, try to render children
			Node child = node.getFirstChild();
			while (child != null) {
				addInline(p, child, fonts, bold, italic);
				child = child.getNext();
			}
		}
	}

	private String extractText(Node node) {
		StringBuilder sb = new StringBuilder();
		Node child = node.getFirstChild();
		while (child != null) {
			if (child instanceof org.commonmark.node.Text text) {
				sb.append(text.getLiteral());
			} else {
				sb.append(extractText(child));
			}
			child = child.getNext();
		}
		return sb.toString();
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
}
