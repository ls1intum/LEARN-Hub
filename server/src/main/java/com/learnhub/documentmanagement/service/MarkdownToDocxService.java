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
	private static final DateTimeFormatter FOOTER_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

	/** A4 landscape dimensions in twentieths of a point (twips). */
	private static final BigInteger PAGE_WIDTH_LANDSCAPE = BigInteger.valueOf(16838);
	private static final BigInteger PAGE_HEIGHT_LANDSCAPE = BigInteger.valueOf(11906);

	/** Page margins in twips (matching CSS margins: 55pt top, 30pt sides, 50pt bottom). */
	private static final BigInteger MARGIN_TOP = BigInteger.valueOf(1100);
	private static final BigInteger MARGIN_BOTTOM = BigInteger.valueOf(1000);
	private static final BigInteger MARGIN_LEFT = BigInteger.valueOf(600);
	private static final BigInteger MARGIN_RIGHT = BigInteger.valueOf(600);
	private static final BigInteger HEADER_FOOTER_MARGIN = BigInteger.valueOf(720);

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
		try {
			// 1. Generate styled HTML from markdown (shared pipeline with PDF)
			String html = markdownToHtmlService.renderMarkdownToDocxHtml(markdown);

			// 2. Create DOCX package and configure page layout
			WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
			SectPr sectPr = configurePage(wordMLPackage, landscape);

			// 3. Convert HTML body content to DOCX elements
			XHTMLImporterImpl importer = new XHTMLImporterImpl(wordMLPackage);
			List<Object> elements = importer.convert(html, null);
			wordMLPackage.getMainDocumentPart().getContent().addAll(elements);

			// 4. Add header (activity name + logo) and footer (date, branding, page numbers)
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

	// ---- Page configuration ----

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

		HeaderReference headerRef = WML_FACTORY.createHeaderReference();
		headerRef.setId(headerRel.getId());
		headerRef.setType(HdrFtrRef.DEFAULT);
		sectPr.getEGHdrFtrReferences().add(headerRef);

		// Footer
		FooterPart footerPart = new FooterPart();
		footerPart.setJaxbElement(createFooter());
		Relationship footerRel = wordMLPackage.getMainDocumentPart().addTargetPart(footerPart);

		FooterReference footerRef = WML_FACTORY.createFooterReference();
		footerRef.setId(footerRel.getId());
		footerRef.setType(HdrFtrRef.DEFAULT);
		sectPr.getEGHdrFtrReferences().add(footerRef);
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
			// Logo dimensions: width=110pt (~1397000 EMU), height=24pt (~304800 EMU)
			Inline inline = imagePart.createImageInline("header-logo", "LEARN-Hub Logo", 1, 2, 1397000, false);
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
		row.getContent()
				.add(createFooterCell(
						"LEARN-Hub \u2013 a TUM Applied Education Technologies application \u00B7 aet.cit.tum.de",
						JcEnumeration.CENTER));

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
