package com.learnhub.documentmanagement.service;

import jakarta.xml.bind.JAXBElement;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.List;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 *
 * <p>
 * Header/footer creation is delegated to {@link DocxHeaderFooterHelper} and
 * table layout to {@link DocxTableHelper}.
 * </p>
 */
@Service
public class MarkdownToDocxService {

	private static final Logger logger = LoggerFactory.getLogger(MarkdownToDocxService.class);
	private static final String SECTION_BREAK_NEXT_PAGE = "nextPage";

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

	private static final ObjectFactory WML_FACTORY = Context.getWmlObjectFactory();

	private final MarkdownToHtmlService markdownToHtmlService;
	private final DocxHeaderFooterHelper headerFooterHelper;
	private final DocxTableHelper tableHelper;

	public MarkdownToDocxService(MarkdownToHtmlService markdownToHtmlService, DocxHeaderFooterHelper headerFooterHelper,
			DocxTableHelper tableHelper) {
		this.markdownToHtmlService = markdownToHtmlService;
		this.headerFooterHelper = headerFooterHelper;
		this.tableHelper = tableHelper;
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
			WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
			SectPr sectPr = configurePage(wordMLPackage, landscape);

			List<Object> elements = importMarkdownSection(wordMLPackage, markdown, landscape);
			wordMLPackage.getMainDocumentPart().getContent().addAll(elements);

			headerFooterHelper.configureHeaderAndFooter(wordMLPackage, sectPr, activityName);

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
	 *            the markdown contents, in order
	 * @param landscapes
	 *            whether each section is landscape, in the same order
	 * @param activityName
	 *            the activity name shown in the page header
	 */
	public byte[] renderMergedDocx(List<String> markdowns, List<Boolean> landscapes, String activityName) {
		if (markdowns.size() != landscapes.size()) {
			throw new IllegalArgumentException("markdowns and landscapes lists must have the same size");
		}
		try {
			WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();

			// Create header/footer parts once — they will be referenced by every section
			String[] relIds = headerFooterHelper.createSharedHeaderFooter(wordMLPackage, activityName);
			String headerRelId = relIds[0];
			String footerRelId = relIds[1];

			for (int i = 0; i < markdowns.size(); i++) {
				List<Object> elements = importMarkdownSection(wordMLPackage, markdowns.get(i), landscapes.get(i));
				wordMLPackage.getMainDocumentPart().getContent().addAll(elements);

				// Add a section break after each section EXCEPT the last one.
				// The last section's properties are set in the body-level SectPr.
				if (i < markdowns.size() - 1) {
					addSectionBreak(wordMLPackage, landscapes.get(i), headerRelId, footerRelId);
				}
			}

			// Configure the body-level SectPr for the last section's orientation
			boolean lastLandscape = landscapes.get(landscapes.size() - 1);
			SectPr sectPr = configurePage(wordMLPackage, lastLandscape);
			headerFooterHelper.addHeaderFooterReferences(sectPr, headerRelId, footerRelId);

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

	private List<Object> importMarkdownSection(WordprocessingMLPackage wordMLPackage, String markdown,
			boolean landscape) throws Exception {
		String html = markdownToHtmlService.renderMarkdownToDocxHtml(markdown);
		// XHTMLImporterImpl keeps conversion state across calls, so a fresh importer
		// is required per section to avoid re-importing earlier content.
		XHTMLImporterImpl importer = new XHTMLImporterImpl(wordMLPackage);
		List<Object> elements = importer.convert(html, null);
		if (isArtikulationsschemaMarkdown(markdown)) {
			tableHelper.applyArtikulationsschemaTableLayout(elements, landscape, PAGE_WIDTH_LANDSCAPE.intValue(),
					PAGE_HEIGHT_LANDSCAPE.intValue(), MARGIN_LEFT.intValue(), MARGIN_RIGHT.intValue());
		}
		return elements;
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
		applyPageGeometry(sectPr, landscape);

		sectPr.setType(WML_FACTORY.createSectPrType());
		sectPr.getType().setVal(SECTION_BREAK_NEXT_PAGE);

		// Reference the shared header/footer so they appear in this section too
		headerFooterHelper.addHeaderFooterReferences(sectPr, headerRelId, footerRelId);

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

	private SectPr configurePage(WordprocessingMLPackage wordMLPackage, boolean landscape) {
		Body body = wordMLPackage.getMainDocumentPart().getJaxbElement().getBody();
		SectPr sectPr = body.getSectPr();
		if (sectPr == null) {
			sectPr = WML_FACTORY.createSectPr();
			body.setSectPr(sectPr);
		}
		applyPageGeometry(sectPr, landscape);

		return sectPr;
	}

	private void applyPageGeometry(SectPr sectPr, boolean landscape) {
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
	}
}
