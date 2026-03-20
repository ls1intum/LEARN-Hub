package com.learnhub.documentmanagement.service;

import java.io.InputStream;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.docx4j.openpackaging.parts.WordprocessingML.FooterPart;
import org.docx4j.openpackaging.parts.WordprocessingML.HeaderPart;
import org.docx4j.relationships.Relationship;
import org.docx4j.wml.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Helper for building DOCX headers and footers via the docx4j API.
 *
 * <p>
 * Extracted from {@link MarkdownToDocxService} to keep the main service class
 * focused on orchestration.
 * </p>
 */
@Component
public class DocxHeaderFooterHelper {

	private static final String LOGO_PATH = "templates/markdown/header-logo.png";
	private static final DateTimeFormatter FOOTER_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
	private static final String FOOTER_BRANDING_TEXT = "LEARN-Hub \u2013 a TUM Applied Education Technologies application \u00B7 aet.cit.tum.de";

	/**
	 * Logo dimensions in EMU. Matches the PDF CSS (height: 22pt). The source PNG is
	 * 107×112 pixels, so width is calculated from the aspect ratio.
	 */
	private static final long LOGO_HEIGHT_EMU = 279400; // 22pt × 12700 EMU/pt
	private static final long LOGO_WIDTH_EMU = 267208; // 22pt × (107/112) × 12700 EMU/pt

	private static final ObjectFactory WML_FACTORY = Context.getWmlObjectFactory();

	/**
	 * Configure header and footer on a single-section document by creating
	 * header/footer parts and attaching them to the given {@link SectPr}.
	 */
	public void configureHeaderAndFooter(WordprocessingMLPackage wordMLPackage, SectPr sectPr, String activityName)
			throws Exception {
		HeaderPart headerPart = new HeaderPart();
		Relationship headerRel = wordMLPackage.getMainDocumentPart().addTargetPart(headerPart);
		headerPart.setJaxbElement(createHeader(wordMLPackage, headerPart, activityName));

		FooterPart footerPart = new FooterPart();
		footerPart.setJaxbElement(createFooter());
		Relationship footerRel = wordMLPackage.getMainDocumentPart().addTargetPart(footerPart);

		addHeaderFooterReferences(sectPr, headerRel.getId(), footerRel.getId());
	}

	/**
	 * Create a header/footer pair and return their relationship IDs for use with
	 * merged (multi-section) documents.
	 *
	 * @return a two-element array: [headerRelId, footerRelId]
	 */
	public String[] createSharedHeaderFooter(WordprocessingMLPackage wordMLPackage, String activityName)
			throws Exception {
		HeaderPart headerPart = new HeaderPart();
		Relationship headerRel = wordMLPackage.getMainDocumentPart().addTargetPart(headerPart);
		headerPart.setJaxbElement(createHeader(wordMLPackage, headerPart, activityName));

		FooterPart footerPart = new FooterPart();
		footerPart.setJaxbElement(createFooter());
		Relationship footerRel = wordMLPackage.getMainDocumentPart().addTargetPart(footerPart);

		return new String[]{headerRel.getId(), footerRel.getId()};
	}

	/**
	 * Add header and footer references to a section properties element. This
	 * ensures headers and footers are displayed in the section controlled by this
	 * SectPr.
	 */
	public void addHeaderFooterReferences(SectPr sectPr, String headerRelId, String footerRelId) {
		HeaderReference headerRef = WML_FACTORY.createHeaderReference();
		headerRef.setId(headerRelId);
		headerRef.setType(HdrFtrRef.DEFAULT);
		sectPr.getEGHdrFtrReferences().add(headerRef);

		FooterReference footerRef = WML_FACTORY.createFooterReference();
		footerRef.setId(footerRelId);
		footerRef.setType(HdrFtrRef.DEFAULT);
		sectPr.getEGHdrFtrReferences().add(footerRef);
	}

	// ---- internal ----

	Hdr createHeader(WordprocessingMLPackage wordMLPackage, HeaderPart headerPart, String activityName)
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

	Ftr createFooter() {
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

	R createStyledRun(String text, int fontSizePt, String color) {
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
	List<R> createFieldRuns(String fieldInstruction, int fontSizePt, String color) {
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
		jakarta.xml.bind.JAXBElement<FldChar> beginEl = WML_FACTORY.createRFldChar(begin);
		beginRun.getContent().add(beginEl);

		// INSTRUCTION
		R instrRun = WML_FACTORY.createR();
		instrRun.setRPr(rPr);
		Text instrText = WML_FACTORY.createText();
		instrText.setValue(" " + fieldInstruction + " ");
		instrText.setSpace("preserve");
		jakarta.xml.bind.JAXBElement<Text> instrEl = WML_FACTORY.createRInstrText(instrText);
		instrRun.getContent().add(instrEl);

		// END
		R endRun = WML_FACTORY.createR();
		endRun.setRPr(rPr);
		FldChar end = WML_FACTORY.createFldChar();
		end.setFldCharType(STFldCharType.END);
		jakarta.xml.bind.JAXBElement<FldChar> endEl = WML_FACTORY.createRFldChar(end);
		endRun.getContent().add(endEl);

		return List.of(beginRun, instrRun, endRun);
	}
}
