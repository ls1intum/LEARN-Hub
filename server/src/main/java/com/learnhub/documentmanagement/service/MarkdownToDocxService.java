package com.learnhub.documentmanagement.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import jakarta.xml.bind.JAXBElement;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.docx4j.openpackaging.parts.WordprocessingML.FooterPart;
import org.docx4j.openpackaging.parts.WordprocessingML.HeaderPart;
import org.docx4j.relationships.Relationship;
import org.docx4j.wml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Service for rendering Markdown content to DOCX (Word) format using docx4j
 * with HTML-based conversion. Uses {@link MarkdownToHtmlService} to generate
 * styled HTML from Markdown, then converts it to DOCX via
 * {@link XHTMLImporterImpl}. Headers and footers are added via the docx4j API
 * since DOCX does not support CSS-based running elements.
 *
 * <p>
 * This approach shares the HTML rendering pipeline with
 * {@link MarkdownToPdfService}, using CSS templates for consistent styling
 * across both PDF and DOCX output formats.
 * </p>
 */
@Service
public class MarkdownToDocxService {

	private static final Logger logger = LoggerFactory.getLogger(MarkdownToDocxService.class);
	private static final String LOGO_PATH = "templates/markdown/header-logo.png";
	private static final String SECTION_BREAK_NEXT_PAGE = "nextPage";
	private static final DateTimeFormatter FOOTER_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
	private static final String FOOTER_BRANDING_TEXT = "LEARN-Hub \u2013 a TUM Applied Education Technologies application \u00B7 aet.cit.tum.de";
	private static final int[] ARTIKULATIONSSCHEMA_COLUMN_WIDTHS_PERCENT = { 8, 12, 38, 10, 16, 16 };

	/** A4 landscape dimensions in twentieths of a point (twips). */
	private static final BigInteger PAGE_WIDTH_LANDSCAPE = BigInteger.valueOf(16838);
	private static final BigInteger PAGE_HEIGHT_LANDSCAPE = BigInteger.valueOf(11906);

	/**
	 * Page margins in twips. Kept tighter in DOCX to use more vertical page space.
	 */
	private static final BigInteger MARGIN_TOP = BigInteger.valueOf(300);
	private static final BigInteger MARGIN_BOTTOM = BigInteger.valueOf(300);
	private static final BigInteger MARGIN_LEFT = BigInteger.valueOf(600);
	private static final BigInteger MARGIN_RIGHT = BigInteger.valueOf(600);
	private static final BigInteger HEADER_FOOTER_MARGIN = BigInteger.valueOf(300);

	/**
	 * Logo dimensions in EMU. Matches the PDF CSS (height: 22pt). The source PNG is
	 * 107×112 pixels, so width is calculated from the aspect ratio.
	 */
	private static final long LOGO_HEIGHT_EMU = 279400; // 22pt × 12700 EMU/pt
	private static final long LOGO_WIDTH_EMU = 267208; // 22pt × (107/112) × 12700 EMU/pt

	private static final ObjectFactory WML_FACTORY = Context.getWmlObjectFactory();

	private final MarkdownToHtmlService markdownToHtmlService;

	public MarkdownToDocxService(MarkdownToHtmlService markdownToHtmlService) {
		this.markdownToHtmlService = markdownToHtmlService;
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
	 *                  the markdown content
	 * @param landscape
	 *                  true for landscape, false for portrait
	 */
	public byte[] renderMarkdownToDocx(String markdown, boolean landscape) {
		return renderMarkdownToDocx(markdown, landscape, "");
	}

	/**
	 * Render markdown content to DOCX bytes with specified orientation and activity
	 * name in the header.
	 *
	 * @param markdown
	 *                     the markdown content
	 * @param landscape
	 *                     true for landscape, false for portrait
	 * @param activityName
	 *                     the activity name shown in the page header
	 */
	public byte[] renderMarkdownToDocx(String markdown, boolean landscape, String activityName) {
		try {
			// 1. Generate styled HTML from markdown (shared pipeline with PDF)
			String html = markdownToHtmlService.renderMarkdownToDocxHtml(markdown);

			// 2. Create DOCX package and configure page layout
			WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
			SectPr sectPr = configurePage(wordMLPackage, landscape);

			// 3. Convert HTML body content to DOCX elements
			XHTMLImporterImpl importer = new XHTMLImporterImpl(wordMLPackage);
			List<Object> elements = importer.convert(html, null);
			if (isArtikulationsschemaMarkdown(markdown)) {
				applyArtikulationsschemaTableLayout(elements, landscape);
			}
			wordMLPackage.getMainDocumentPart().getContent().addAll(elements);

			// 4. Add header (activity name + logo) and footer (date, branding, page
			// numbers)
			configureHeaderAndFooter(wordMLPackage, sectPr, activityName);

			// 5. Write to bytes
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				wordMLPackage.save(baos);
				return baos.toByteArray();
			}
		} catch (Exception e) {
			logger.error("Failed to render markdown to DOCX: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to render markdown to DOCX: " + e.getMessage(), e);
		}
	}

	/**
	 * Render multiple markdown sections into a single DOCX with per-section
	 * orientation (portrait/landscape). Each section is separated by a section
	 * break (next page). Headers and footers appear on all pages across all
	 * sections.
	 *
	 * @param markdowns
	 *                     the markdown contents, in order
	 * @param landscapes
	 *                     whether each section is landscape, in the same order
	 * @param activityName
	 *                     the activity name shown in the page header
	 */
	public byte[] renderMergedDocx(List<String> markdowns, List<Boolean> landscapes, String activityName) {
		if (markdowns.size() != landscapes.size()) {
			throw new IllegalArgumentException("markdowns and landscapes lists must have the same size");
		}
		try {
			WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();

			// Create header/footer parts once — they will be referenced by every section
			HeaderPart headerPart = new HeaderPart();
			Relationship headerRel = wordMLPackage.getMainDocumentPart().addTargetPart(headerPart);
			headerPart.setJaxbElement(createHeader(wordMLPackage, headerPart, activityName));

			FooterPart footerPart = new FooterPart();
			footerPart.setJaxbElement(createFooter());
			Relationship footerRel = wordMLPackage.getMainDocumentPart().addTargetPart(footerPart);

			for (int i = 0; i < markdowns.size(); i++) {
				String html = markdownToHtmlService.renderMarkdownToDocxHtml(markdowns.get(i));
				// XHTMLImporterImpl keeps conversion state across calls, so a fresh
				// importer is required per section to avoid re-importing earlier content.
				XHTMLImporterImpl importer = new XHTMLImporterImpl(wordMLPackage);
				List<Object> elements = importer.convert(html, null);
				if (isArtikulationsschemaMarkdown(markdowns.get(i))) {
					applyArtikulationsschemaTableLayout(elements, landscapes.get(i));
				}
				wordMLPackage.getMainDocumentPart().getContent().addAll(elements);

				// Add a section break after each section EXCEPT the last one.
				// The last section's properties are set in the body-level SectPr.
				if (i < markdowns.size() - 1) {
					addSectionBreak(wordMLPackage, landscapes.get(i), headerRel.getId(), footerRel.getId());
				}
			}

			// Configure the body-level SectPr for the last section's orientation
			boolean lastLandscape = landscapes.get(landscapes.size() - 1);
			SectPr sectPr = configurePage(wordMLPackage, lastLandscape);
			addHeaderFooterReferences(sectPr, headerRel.getId(), footerRel.getId());

			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				wordMLPackage.save(baos);
				return baos.toByteArray();
			}
		} catch (Exception e) {
			logger.error("Failed to render merged markdown to DOCX: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to render merged markdown to DOCX: " + e.getMessage(), e);
		}
	}

	// ---- Page configuration ----

	private boolean isArtikulationsschemaMarkdown(String markdown) {
		return markdown != null && markdown.matches("(?is)^\\s*#\\s+Artikulationsschema\\b.*");
	}

	private void applyArtikulationsschemaTableLayout(List<Object> elements, boolean landscape) {
		int totalWidthTwips = (landscape ? PAGE_WIDTH_LANDSCAPE.intValue() : PAGE_HEIGHT_LANDSCAPE.intValue())
				- MARGIN_LEFT.intValue() - MARGIN_RIGHT.intValue();
		int[] columnWidths = toAbsoluteColumnWidths(totalWidthTwips);

		for (Object element : elements) {
			Object value = unwrap(element);
			if (value instanceof Tbl table) {
				configureFullWidthTable(table, totalWidthTwips, columnWidths);
			}
		}
	}

	private int[] toAbsoluteColumnWidths(int totalWidthTwips) {
		int[] widths = new int[ARTIKULATIONSSCHEMA_COLUMN_WIDTHS_PERCENT.length];
		int consumed = 0;

		for (int i = 0; i < ARTIKULATIONSSCHEMA_COLUMN_WIDTHS_PERCENT.length; i++) {
			if (i == ARTIKULATIONSSCHEMA_COLUMN_WIDTHS_PERCENT.length - 1) {
				widths[i] = totalWidthTwips - consumed;
			} else {
				widths[i] = totalWidthTwips * ARTIKULATIONSSCHEMA_COLUMN_WIDTHS_PERCENT[i] / 100;
				consumed += widths[i];
			}
		}

		return widths;
	}

	private void configureFullWidthTable(Tbl table, int totalWidthTwips, int[] columnWidths) {
		TblPr tblPr = table.getTblPr();
		if (tblPr == null) {
			tblPr = WML_FACTORY.createTblPr();
			table.setTblPr(tblPr);
		}

		TblWidth tblWidth = WML_FACTORY.createTblWidth();
		tblWidth.setType(TblWidth.TYPE_DXA);
		tblWidth.setW(BigInteger.valueOf(totalWidthTwips));
		tblPr.setTblW(tblWidth);

		CTTblLayoutType layout = WML_FACTORY.createCTTblLayoutType();
		layout.setType(STTblLayoutType.FIXED);
		tblPr.setTblLayout(layout);

		TblGrid tblGrid = table.getTblGrid();
		if (tblGrid == null) {
			tblGrid = WML_FACTORY.createTblGrid();
			table.setTblGrid(tblGrid);
		}
		tblGrid.getGridCol().clear();
		for (int width : columnWidths) {
			TblGridCol gridCol = WML_FACTORY.createTblGridCol();
			gridCol.setW(BigInteger.valueOf(width));
			tblGrid.getGridCol().add(gridCol);
		}

		for (Object rowObject : table.getContent()) {
			Object rowValue = unwrap(rowObject);
			if (!(rowValue instanceof Tr row)) {
				continue;
			}

			int columnIndex = 0;
			for (Object cellObject : row.getContent()) {
				Object cellValue = unwrap(cellObject);
				if (!(cellValue instanceof Tc cell)) {
					continue;
				}

				int span = getGridSpan(cell);
				int cellWidthTwips = 0;
				for (int i = 0; i < span && columnIndex + i < columnWidths.length; i++) {
					cellWidthTwips += columnWidths[columnIndex + i];
				}
				setCellWidth(cell, cellWidthTwips);
				columnIndex += Math.max(span, 1);
			}
		}
	}

	private int getGridSpan(Tc cell) {
		TcPr tcPr = cell.getTcPr();
		if (tcPr == null || tcPr.getGridSpan() == null || tcPr.getGridSpan().getVal() == null) {
			return 1;
		}
		return Math.max(tcPr.getGridSpan().getVal().intValue(), 1);
	}

	private void setCellWidth(Tc cell, int widthTwips) {
		TcPr tcPr = cell.getTcPr();
		if (tcPr == null) {
			tcPr = WML_FACTORY.createTcPr();
			cell.setTcPr(tcPr);
		}

		TblWidth tcWidth = WML_FACTORY.createTblWidth();
		tcWidth.setType(TblWidth.TYPE_DXA);
		tcWidth.setW(BigInteger.valueOf(widthTwips));
		tcPr.setTcW(tcWidth);
	}

	private Object unwrap(Object value) {
		return value instanceof JAXBElement<?> element ? element.getValue() : value;
	}

	/**
	 * Insert a section break (next page) after the current content, setting the
	 * given orientation. In DOCX, a mid-document section break is stored as a
	 * {@link SectPr} inside the last paragraph's {@link PPr}. Header/footer
	 * references are added so every section displays them.
	 */
	private void addSectionBreak(WordprocessingMLPackage wordMLPackage, boolean landscape, String headerRelId,
			String footerRelId) {
		SectPr sectPr = WML_FACTORY.createSectPr();

		SectPr.PgSz pgSz = WML_FACTORY.createSectPrPgSz();
		if (landscape) {
			pgSz.setW(PAGE_WIDTH_LANDSCAPE);
			pgSz.setH(PAGE_HEIGHT_LANDSCAPE);
			pgSz.setOrient(STPageOrientation.LANDSCAPE);
		} else {
			pgSz.setW(PAGE_HEIGHT_LANDSCAPE);
			pgSz.setH(PAGE_WIDTH_LANDSCAPE);
			pgSz.setOrient(STPageOrientation.PORTRAIT);
		}
		sectPr.setPgSz(pgSz);

		SectPr.PgMar pgMar = WML_FACTORY.createSectPrPgMar();
		pgMar.setTop(MARGIN_TOP);
		pgMar.setBottom(MARGIN_BOTTOM);
		pgMar.setLeft(MARGIN_LEFT);
		pgMar.setRight(MARGIN_RIGHT);
		pgMar.setHeader(HEADER_FOOTER_MARGIN);
		pgMar.setFooter(HEADER_FOOTER_MARGIN);
		sectPr.setPgMar(pgMar);

		sectPr.setType(WML_FACTORY.createSectPrType());
		sectPr.getType().setVal(SECTION_BREAK_NEXT_PAGE);

		// Reference the shared header/footer so they appear in this section too
		addHeaderFooterReferences(sectPr, headerRelId, footerRelId);

		// In DOCX, a mid-document section break must be attached to the last
		// paragraph of the current section. Adding a separate trailing paragraph can
		// cause Word to lay out the document incorrectly across section boundaries.
		List<Object> content = wordMLPackage.getMainDocumentPart().getContent();
		P targetParagraph;

		if (content.isEmpty()) {
			targetParagraph = WML_FACTORY.createP();
			content.add(targetParagraph);
		} else {
			Object lastItem = content.get(content.size() - 1);
			Object lastValue = lastItem instanceof JAXBElement<?> element ? element.getValue() : lastItem;

			if (lastValue instanceof P paragraph) {
				targetParagraph = paragraph;
			} else {
				// If the section ends in a table or another block type, place an empty
				// paragraph after it and attach the section break there so all preceding
				// content stays in the current section.
				targetParagraph = WML_FACTORY.createP();
				content.add(targetParagraph);
			}
		}

		PPr pPr = targetParagraph.getPPr();
		if (pPr == null) {
			pPr = WML_FACTORY.createPPr();
			targetParagraph.setPPr(pPr);
		}
		pPr.setSectPr(sectPr);
	}

	/**
	 * Add header and footer references to a section properties element. This
	 * ensures headers and footers are displayed in the section controlled by this
	 * SectPr.
	 */
	private void addHeaderFooterReferences(SectPr sectPr, String headerRelId, String footerRelId) {
		HeaderReference headerRef = WML_FACTORY.createHeaderReference();
		headerRef.setId(headerRelId);
		headerRef.setType(HdrFtrRef.DEFAULT);
		sectPr.getEGHdrFtrReferences().add(headerRef);

		FooterReference footerRef = WML_FACTORY.createFooterReference();
		footerRef.setId(footerRelId);
		footerRef.setType(HdrFtrRef.DEFAULT);
		sectPr.getEGHdrFtrReferences().add(footerRef);
	}

	private SectPr configurePage(WordprocessingMLPackage wordMLPackage, boolean landscape) {
		Body body = wordMLPackage.getMainDocumentPart().getJaxbElement().getBody();
		SectPr sectPr = body.getSectPr();
		if (sectPr == null) {
			sectPr = WML_FACTORY.createSectPr();
			body.setSectPr(sectPr);
		}

		SectPr.PgSz pgSz = WML_FACTORY.createSectPrPgSz();
		if (landscape) {
			pgSz.setW(PAGE_WIDTH_LANDSCAPE);
			pgSz.setH(PAGE_HEIGHT_LANDSCAPE);
			pgSz.setOrient(STPageOrientation.LANDSCAPE);
		} else {
			pgSz.setW(PAGE_HEIGHT_LANDSCAPE);
			pgSz.setH(PAGE_WIDTH_LANDSCAPE);
			pgSz.setOrient(STPageOrientation.PORTRAIT);
		}
		sectPr.setPgSz(pgSz);

		SectPr.PgMar pgMar = WML_FACTORY.createSectPrPgMar();
		pgMar.setTop(MARGIN_TOP);
		pgMar.setBottom(MARGIN_BOTTOM);
		pgMar.setLeft(MARGIN_LEFT);
		pgMar.setRight(MARGIN_RIGHT);
		pgMar.setHeader(HEADER_FOOTER_MARGIN);
		pgMar.setFooter(HEADER_FOOTER_MARGIN);
		sectPr.setPgMar(pgMar);

		return sectPr;
	}

	// ---- Header and footer ----

	private void configureHeaderAndFooter(WordprocessingMLPackage wordMLPackage, SectPr sectPr, String activityName)
			throws Exception {
		// Header — add part to document FIRST so images can be created
		HeaderPart headerPart = new HeaderPart();
		Relationship headerRel = wordMLPackage.getMainDocumentPart().addTargetPart(headerPart);
		headerPart.setJaxbElement(createHeader(wordMLPackage, headerPart, activityName));

		// Footer
		FooterPart footerPart = new FooterPart();
		footerPart.setJaxbElement(createFooter());
		Relationship footerRel = wordMLPackage.getMainDocumentPart().addTargetPart(footerPart);

		addHeaderFooterReferences(sectPr, headerRel.getId(), footerRel.getId());
	}

	private Hdr createHeader(WordprocessingMLPackage wordMLPackage, HeaderPart headerPart, String activityName)
			throws Exception {
		Hdr header = WML_FACTORY.createHdr();

		P paragraph = WML_FACTORY.createP();

		// Right-align the header paragraph
		PPr pPr = WML_FACTORY.createPPr();
		Jc jc = WML_FACTORY.createJc();
		jc.setVal(JcEnumeration.RIGHT);
		pPr.setJc(jc);
		paragraph.setPPr(pPr);

		// Activity name text
		R textRun = createStyledRun(activityName != null ? activityName : "", 10, "555555");
		paragraph.getContent().add(textRun);

		// Spacer
		R spacerRun = WML_FACTORY.createR();
		Text spacer = WML_FACTORY.createText();
		spacer.setValue("  ");
		spacer.setSpace("preserve");
		spacerRun.getContent().add(spacer);
		paragraph.getContent().add(spacerRun);

		// Logo image
		try (InputStream logoStream = new ClassPathResource(LOGO_PATH).getInputStream()) {
			byte[] logoBytes = logoStream.readAllBytes();
			BinaryPartAbstractImage imagePart = BinaryPartAbstractImage.createImagePart(wordMLPackage, headerPart,
					logoBytes);
			Inline inline = imagePart.createImageInline("header-logo", "LEARN-Hub Logo", 1, 2, false);
			// Manually set logo dimensions (createImageInline miscalculates for PNGs
			// without DPI metadata)
			inline.getExtent().setCx(LOGO_WIDTH_EMU);
			inline.getExtent().setCy(LOGO_HEIGHT_EMU);
			Drawing drawing = WML_FACTORY.createDrawing();
			drawing.getAnchorOrInline().add(inline);
			R imageRun = WML_FACTORY.createR();
			imageRun.getContent().add(drawing);
			paragraph.getContent().add(imageRun);
		}

		header.getContent().add(paragraph);
		return header;
	}

	private Ftr createFooter() {
		Ftr footer = WML_FACTORY.createFtr();

		// Footer uses a 3-column table: date (left) | branding (center) | page number
		// (right)
		Tbl table = WML_FACTORY.createTbl();
		configureFooterTableProperties(table);

		Tr row = WML_FACTORY.createTr();

		// Left cell: download date
		row.getContent().add(createFooterCell(LocalDateTime.now().format(FOOTER_DATE_FORMATTER), JcEnumeration.LEFT));

		// Center cell: branding text
		row.getContent().add(createFooterCell(FOOTER_BRANDING_TEXT, JcEnumeration.CENTER));

		// Right cell: page number
		row.getContent().add(createFooterPageNumberCell());

		table.getContent().add(row);
		footer.getContent().add(table);
		return footer;
	}

	private void configureFooterTableProperties(Tbl table) {
		TblPr tblPr = WML_FACTORY.createTblPr();

		// Full width table
		TblWidth tblWidth = WML_FACTORY.createTblWidth();
		tblWidth.setType("pct");
		tblWidth.setW(BigInteger.valueOf(5000));
		tblPr.setTblW(tblWidth);

		// No borders
		TblBorders borders = WML_FACTORY.createTblBorders();
		borders.setTop(createNilBorder());
		borders.setBottom(createNilBorder());
		borders.setLeft(createNilBorder());
		borders.setRight(createNilBorder());
		borders.setInsideH(createNilBorder());
		borders.setInsideV(createNilBorder());
		tblPr.setTblBorders(borders);

		table.setTblPr(tblPr);
	}

	private CTBorder createNilBorder() {
		CTBorder border = WML_FACTORY.createCTBorder();
		border.setVal(STBorder.NIL);
		return border;
	}

	private Tc createFooterCell(String text, JcEnumeration alignment) {
		Tc cell = WML_FACTORY.createTc();
		P para = WML_FACTORY.createP();

		PPr pPr = WML_FACTORY.createPPr();
		Jc jc = WML_FACTORY.createJc();
		jc.setVal(alignment);
		pPr.setJc(jc);
		para.setPPr(pPr);

		R run = createStyledRun(text, 7, "555555");
		para.getContent().add(run);

		cell.getContent().add(para);
		return cell;
	}

	private Tc createFooterPageNumberCell() {
		Tc cell = WML_FACTORY.createTc();
		P para = WML_FACTORY.createP();

		PPr pPr = WML_FACTORY.createPPr();
		Jc jc = WML_FACTORY.createJc();
		jc.setVal(JcEnumeration.RIGHT);
		pPr.setJc(jc);
		para.setPPr(pPr);

		// "Page "
		para.getContent().add(createStyledRun("Page ", 7, "555555"));

		// PAGE field
		para.getContent().addAll(createFieldRuns("PAGE", 7, "555555"));

		// " of "
		para.getContent().add(createStyledRun(" of ", 7, "555555"));

		// NUMPAGES field
		para.getContent().addAll(createFieldRuns("NUMPAGES", 7, "555555"));

		cell.getContent().add(para);
		return cell;
	}

	// ---- Utility methods ----

	private R createStyledRun(String text, int fontSizePt, String color) {
		R run = WML_FACTORY.createR();

		RPr rPr = WML_FACTORY.createRPr();
		HpsMeasure fontSize = WML_FACTORY.createHpsMeasure();
		fontSize.setVal(BigInteger.valueOf(fontSizePt * 2L)); // half-points
		rPr.setSz(fontSize);
		rPr.setSzCs(fontSize);
		Color c = WML_FACTORY.createColor();
		c.setVal(color);
		rPr.setColor(c);
		run.setRPr(rPr);

		Text t = WML_FACTORY.createText();
		t.setValue(text);
		t.setSpace("preserve");
		run.getContent().add(t);

		return run;
	}

	/**
	 * Create the run sequence for a Word field code (BEGIN + INSTR + END).
	 */
	private List<R> createFieldRuns(String fieldInstruction, int fontSizePt, String color) {
		RPr rPr = WML_FACTORY.createRPr();
		HpsMeasure fontSize = WML_FACTORY.createHpsMeasure();
		fontSize.setVal(BigInteger.valueOf(fontSizePt * 2L));
		rPr.setSz(fontSize);
		rPr.setSzCs(fontSize);
		Color c = WML_FACTORY.createColor();
		c.setVal(color);
		rPr.setColor(c);

		// BEGIN
		R beginRun = WML_FACTORY.createR();
		beginRun.setRPr(rPr);
		FldChar begin = WML_FACTORY.createFldChar();
		begin.setFldCharType(STFldCharType.BEGIN);
		JAXBElement<FldChar> beginEl = WML_FACTORY.createRFldChar(begin);
		beginRun.getContent().add(beginEl);

		// INSTRUCTION
		R instrRun = WML_FACTORY.createR();
		instrRun.setRPr(rPr);
		Text instrText = WML_FACTORY.createText();
		instrText.setValue(" " + fieldInstruction + " ");
		instrText.setSpace("preserve");
		JAXBElement<Text> instrEl = WML_FACTORY.createRInstrText(instrText);
		instrRun.getContent().add(instrEl);

		// END
		R endRun = WML_FACTORY.createR();
		endRun.setRPr(rPr);
		FldChar end = WML_FACTORY.createFldChar();
		end.setFldCharType(STFldCharType.END);
		JAXBElement<FldChar> endEl = WML_FACTORY.createRFldChar(end);
		endRun.getContent().add(endEl);

		return List.of(beginRun, instrRun, endRun);
	}
}
