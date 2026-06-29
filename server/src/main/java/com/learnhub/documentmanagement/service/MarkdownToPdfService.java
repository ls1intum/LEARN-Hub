package com.learnhub.documentmanagement.service;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.html2pdf.resolver.font.DefaultFontProvider;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.utils.PdfMerger;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for rendering Markdown content into a styled PDF using iText html2pdf
 * and {@link MarkdownToHtmlService} for the Markdown-to-HTML conversion step.
 */
@Service
public class MarkdownToPdfService {

	private static final Logger logger = LoggerFactory.getLogger(MarkdownToPdfService.class);
	private static final String WORKSHEET_FONT_FAMILY = "Comic Sans MS";
	private static final int MAX_CMAP_RETRY_CHARACTERS = 256;
	// Fonts bundled in src/main/resources/fonts/ — single source of truth for all
	// environments (local dev, Docker). Loaded from classpath at startup.
	private static final List<String> CLASSPATH_FONT_RESOURCES = List.of("/fonts/ComicSansMS.ttf",
			"/fonts/ComicSansMSBold.ttf", "/fonts/NotoEmoji-Regular.ttf");

	// OS-installed fonts tried as additional fallbacks (e.g. DejaVu from Alpine's
	// ttf-dejavu package for arrows/dingbats). Missing paths are silently skipped.
	private static final List<String> WORKSHEET_FONT_PATHS = List.of(
			"/System/Library/Fonts/Supplemental/Comic Sans MS.ttf",
			"/System/Library/Fonts/Supplemental/Comic Sans MS Bold.ttf", "/Library/Fonts/Comic Sans MS.ttf",
			"/Library/Fonts/Comic Sans MS Bold.ttf", "C:/Windows/Fonts/comic.ttf", "C:/Windows/Fonts/comicbd.ttf");

	private static final List<String> SYMBOL_FONT_PATHS = List.of(
			// DejaVu Sans – arrows, checkmarks, dingbats, mathematical symbols
			"/usr/share/fonts/truetype/ttf-dejavu/DejaVuSans.ttf", "/usr/share/fonts/ttf-dejavu/DejaVuSans.ttf",
			"/usr/share/fonts/TTF/DejaVuSans.ttf",
			// macOS – Arial Unicode MS as broad fallback
			"/Library/Fonts/Arial Unicode MS.ttf",
			// Windows – Segoe UI Emoji
			"C:/Windows/Fonts/seguiemj.ttf");

	private final MarkdownToHtmlService markdownToHtmlService;

	public MarkdownToPdfService(MarkdownToHtmlService markdownToHtmlService) {
		this.markdownToHtmlService = markdownToHtmlService;
	}

	/**
	 * Render markdown content to PDF bytes (default landscape).
	 */
	public byte[] renderMarkdownToPdf(String markdown) {
		return renderMarkdownToPdf(markdown, true, "");
	}

	/**
	 * Render markdown content to PDF bytes with specified orientation.
	 *
	 * @param markdown
	 *            the markdown content
	 * @param landscape
	 *            true for landscape, false for portrait
	 */
	public byte[] renderMarkdownToPdf(String markdown, boolean landscape) {
		return renderMarkdownToPdf(markdown, landscape, "");
	}

	/**
	 * Render markdown content to PDF bytes with specified orientation and activity
	 * name in header.
	 *
	 * @param markdown
	 *            the markdown content
	 * @param landscape
	 *            true for landscape, false for portrait
	 * @param activityName
	 *            the activity name shown in the page header
	 */
	public byte[] renderMarkdownToPdf(String markdown, boolean landscape, String activityName) {
		return renderMarkdownToPdf(markdown, landscape, activityName, false);
	}

	/**
	 * Render markdown content to PDF bytes, optionally applying the exercise-sheet
	 * layout (outer border with Name / Datum fields repeated at the top of every
	 * page).
	 */
	public byte[] renderMarkdownToPdf(String markdown, boolean landscape, String activityName, boolean exerciseSheet) {
		return renderMarkdownToPdf(markdown, landscape, activityName, exerciseSheet, false);
	}

	public byte[] renderMarkdownToPdf(String markdown, boolean landscape, String activityName, boolean exerciseSheet,
			boolean isBoardImage) {
		String html = markdownToHtmlService.renderMarkdownToHtml(markdown, landscape, activityName, exerciseSheet,
				isBoardImage);
		IEventHandler eventHandler = exerciseSheet ? new ExerciseSheetEventHandler() : null;
		return renderHtmlDocumentToPdf(html, activityName, eventHandler);
	}

	/**
	 * Render a complete, self-contained HTML document to PDF bytes, applying the
	 * same cmap-retry logic used by the other render methods.
	 */
	public byte[] renderCompleteHtmlToPdf(String completeHtml) {
		return renderHtmlDocumentToPdf(completeHtml, null);
	}

	/**
	 * Render an existing HTML body to PDF bytes with the standard LEARN-Hub PDF
	 * layout.
	 */
	public byte[] renderHtmlToPdf(String htmlBody, boolean landscape, String activityName) {
		String html = markdownToHtmlService.renderHtmlBodyToHtmlDocument(htmlBody, landscape, activityName);
		return renderHtmlDocumentToPdf(html, activityName);
	}

	private byte[] renderHtmlDocumentToPdf(String html) {
		return renderHtmlDocumentToPdf(html, null, null);
	}

	private byte[] renderHtmlDocumentToPdf(String html, String documentTitle) {
		return renderHtmlDocumentToPdf(html, documentTitle, null);
	}

	private byte[] renderHtmlDocumentToPdf(String html, String documentTitle, IEventHandler eventHandler) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			convertHtmlDocumentToPdfBytes(html, baos, createConverterProperties(), eventHandler);
			return applyDocumentTitle(baos.toByteArray(), documentTitle);
		} catch (Exception e) {
			if (isCmapFailure(e)) {
				return retryRenderWithoutProblematicCharacter(html, documentTitle, e, eventHandler);
			}
			logger.error("Failed to render markdown PDF: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to render markdown PDF: " + e.getMessage(), e);
		}
	}

	void convertHtmlDocumentToPdfBytes(String html, ByteArrayOutputStream outputStream,
			ConverterProperties converterProperties) {
		HtmlConverter.convertToPdf(html, outputStream, converterProperties);
	}

	void convertHtmlDocumentToPdfBytes(String html, ByteArrayOutputStream outputStream,
			ConverterProperties converterProperties, IEventHandler eventHandler) {
		if (eventHandler == null) {
			convertHtmlDocumentToPdfBytes(html, outputStream, converterProperties);
			return;
		}
		try (PdfDocument pdfDoc = new PdfDocument(new PdfWriter(outputStream))) {
			pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, eventHandler);
			HtmlConverter.convertToPdf(html, pdfDoc, converterProperties);
		}
	}

	ConverterProperties createConverterProperties() {
		// Use iText's bundled/shipped fonts for Unicode fallback instead of
		// addSystemFonts(), which on macOS/Linux includes Apple-specific fonts with
		// incomplete cmap tables that cause a NullPointerException during rendering.
		DefaultFontProvider fontProvider = new DefaultFontProvider(false, true, false, WORKSHEET_FONT_FAMILY);

		for (String resource : CLASSPATH_FONT_RESOURCES) {
			try (InputStream is = MarkdownToPdfService.class.getResourceAsStream(resource)) {
				if (is != null) {
					fontProvider.addFont(is.readAllBytes());
					logger.debug("Registered classpath font: {}", resource);
				}
			} catch (IOException e) {
				logger.debug("Could not load classpath font {}: {}", resource, e.getMessage());
			}
		}

		boolean worksheetFontRegistered = false;
		for (String fontPath : WORKSHEET_FONT_PATHS) {
			if (!Files.exists(Path.of(fontPath))) {
				continue;
			}
			if (fontProvider.addFont(fontPath)) {
				worksheetFontRegistered = true;
			}
		}

		if (!worksheetFontRegistered) {
			logger.warn(
					"Worksheet PDF font '{}' was not registered from the known font paths. Falling back to other system fonts.",
					WORKSHEET_FONT_FAMILY);
		}

		int symbolFontsRegistered = 0;
		for (String fontPath : SYMBOL_FONT_PATHS) {
			if (!Files.exists(Path.of(fontPath))) {
				continue;
			}
			if (fontProvider.addFont(fontPath)) {
				symbolFontsRegistered++;
				logger.debug("Registered symbol/emoji fallback font: {}", fontPath);
			}
		}
		if (symbolFontsRegistered == 0) {
			logger.debug(
					"No symbol/emoji fallback fonts were found. Characters outside Comic Sans will be dropped on cmap failure.");
		}

		return new ConverterProperties().setFontProvider(fontProvider);
	}

	private byte[] retryRenderWithoutProblematicCharacter(String html, String documentTitle,
			Exception originalException) {
		return retryRenderWithoutProblematicCharacter(html, documentTitle, originalException, null);
	}

	private byte[] retryRenderWithoutProblematicCharacter(String html, String documentTitle,
			Exception originalException, IEventHandler eventHandler) {
		Set<Integer> candidateCodePoints = findProblematicCharacterCandidates(html);
		String currentHtml = html;
		for (int codePoint : candidateCodePoints) {
			String sanitizedHtml = removeCodePoint(currentHtml, codePoint);
			if (sanitizedHtml.equals(currentHtml)) {
				continue;
			}
			currentHtml = sanitizedHtml;

			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				convertHtmlDocumentToPdfBytes(currentHtml, baos, createConverterProperties(), eventHandler);
				logger.warn(
						"Rendered markdown PDF after removing character {} (U+{}) because iText failed with a null cmap table.",
						describeCodePoint(codePoint), formatCodePoint(codePoint));
				return applyDocumentTitle(baos.toByteArray(), documentTitle);
			} catch (Exception retryException) {
				if (!isCmapFailure(retryException)) {
					break;
				}
				logger.debug("Retry after removing character {} (U+{}) still failed: {}", describeCodePoint(codePoint),
						formatCodePoint(codePoint), retryException.getMessage());
			}
		}

		logger.error("Failed to render markdown PDF: {}", originalException.getMessage(), originalException);
		throw new RuntimeException("Failed to render markdown PDF: " + originalException.getMessage(),
				originalException);
	}

	private Set<Integer> findProblematicCharacterCandidates(String html) {
		Set<Integer> candidates = new LinkedHashSet<>();
		html.codePoints().filter(this::shouldRetryWithoutCodePoint).limit(MAX_CMAP_RETRY_CHARACTERS)
				.forEach(candidates::add);
		return candidates;
	}

	private boolean shouldRetryWithoutCodePoint(int codePoint) {
		if (Character.isWhitespace(codePoint)) {
			return false;
		}
		if (codePoint >= 0x20 && codePoint <= 0x7E) {
			return false;
		}
		return !Character.isISOControl(codePoint);
	}

	private String removeCodePoint(String text, int codePoint) {
		return text.codePoints().filter(current -> current != codePoint)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
	}

	private boolean isCmapFailure(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			String message = current.getMessage();
			if (message != null && message.toLowerCase(Locale.ROOT).contains("cmap")) {
				return true;
			}
			// Java 17+ NullPointerExceptions include the field/variable name in their
			// message (e.g. "…because \"this.toUnicode\" is null"), which does NOT
			// always contain "cmap" even when the root cause is a missing cmap table.
			// Treat any NPE whose top stack frame originates from iText's font-encoding
			// package as a cmap failure so the character-dropping retry is triggered.
			if (current instanceof NullPointerException) {
				StackTraceElement[] stack = current.getStackTrace();
				if (stack.length > 0 && stack[0].getClassName().startsWith("com.itextpdf.io.font")) {
					return true;
				}
			}
			current = current.getCause();
		}
		return false;
	}

	private String describeCodePoint(int codePoint) {
		if (Character.isSupplementaryCodePoint(codePoint)) {
			return new String(Character.toChars(codePoint));
		}
		return Character.toString((char) codePoint);
	}

	private String formatCodePoint(int codePoint) {
		return String.format(Locale.ROOT, "%04X", codePoint);
	}

	public byte[] applyDocumentTitle(byte[] pdfBytes, String documentTitle) {
		String normalizedTitle = normalizeDocumentTitle(documentTitle);
		if (normalizedTitle == null || pdfBytes == null || pdfBytes.length == 0) {
			return pdfBytes;
		}

		try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfBytes);
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				PdfDocument pdfDocument = new PdfDocument(new PdfReader(inputStream), new PdfWriter(outputStream))) {
			pdfDocument.getDocumentInfo().setTitle(normalizedTitle);
			pdfDocument.close();
			return outputStream.toByteArray();
		} catch (Exception e) {
			logger.warn("Failed to set PDF document title '{}': {}", normalizedTitle, e.getMessage());
			return pdfBytes;
		}
	}

	/**
	 * Merge multiple PDF byte arrays into a single PDF document.
	 */
	public byte[] mergePdfs(List<byte[]> pdfParts) {
		return mergePdfs(pdfParts, null);
	}

	public byte[] mergePdfs(List<byte[]> pdfParts, String documentTitle) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PdfDocument mergedDoc = new PdfDocument(new PdfWriter(baos))) {
			PdfMerger merger = new PdfMerger(mergedDoc);

			for (byte[] pdfBytes : pdfParts) {
				try (PdfDocument srcDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfBytes)))) {
					merger.merge(srcDoc, 1, srcDoc.getNumberOfPages());
				}
			}

			mergedDoc.close();
			return applyDocumentTitle(baos.toByteArray(), documentTitle);
		} catch (Exception e) {
			logger.error("Failed to merge PDFs: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to merge PDFs: " + e.getMessage(), e);
		}
	}

	private String normalizeDocumentTitle(String title) {
		if (title == null) {
			return null;
		}

		String normalized = title.trim();
		return normalized.isEmpty() ? null : normalized;
	}

	/**
	 * Draws the exercise-sheet decoration onto each rendered page:
	 * <ul>
	 * <li>An outer border rectangle on every page.</li>
	 * <li>A Name / Datum fill-in row (with separator line) on every <em>odd</em>
	 * page.</li>
	 * </ul>
	 * The CSS for exercise sheets adds {@code EXERCISE_EXTRA_TOP_MARGIN_PT} to the
	 * standard top margin, creating blank space at the top of each page where the
	 * Name/Datum row is drawn on odd pages.
	 */
	private static final class ExerciseSheetEventHandler implements IEventHandler {

		// Must match MarkdownToHtmlService.EXERCISE_SHEET_EXTRA_TOP_MARGIN_PT
		private static final float EXTRA_TOP_MARGIN = 22f;
		private static final float STANDARD_TOP_MARGIN = 55f;
		// Must match MarkdownToHtmlService.EXERCISE_BORDER_TOP_INSET_PT
		private static final float BORDER_TOP_INSET = 4f;

		private final PdfFont boldFont;
		private final PdfFont regularFont;

		ExerciseSheetEventHandler() {
			try {
				this.boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
				this.regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
			} catch (IOException e) {
				throw new RuntimeException("Failed to load fonts for exercise sheet header", e);
			}
		}

		@Override
		public void handleEvent(Event event) {
			PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
			PdfDocument pdfDoc = docEvent.getDocument();
			PdfPage page = docEvent.getPage();
			int pageNumber = pdfDoc.getPageNumber(page);
			Rectangle pageSize = page.getPageSize();

			boolean landscape = pageSize.getWidth() > pageSize.getHeight();
			float marginLeft = landscape ? 30f : 35f;
			float marginRight = landscape ? 30f : 35f;
			float marginBottom = landscape ? 50f : 55f;

			float borderLeft = marginLeft;
			float borderRight = pageSize.getWidth() - marginRight;
			float borderBottom = marginBottom;
			float borderTop = pageSize.getHeight() - STANDARD_TOP_MARGIN - BORDER_TOP_INSET;
			float nameRowBottom = borderTop - EXTRA_TOP_MARGIN;

			PdfCanvas canvas = new PdfCanvas(page.newContentStreamAfter(), page.getResources(), pdfDoc);

			// Outer border on every page
			canvas.saveState().setStrokeColor(new DeviceRgb(0x55, 0x55, 0x55)).setLineWidth(1.5f)
					.rectangle(borderLeft, borderBottom, borderRight - borderLeft, borderTop - borderBottom).stroke()
					.restoreState();

			if (pageNumber % 2 == 1) {
				// Separator line between Name/Datum row and content
				canvas.saveState().setStrokeColor(new DeviceRgb(0x88, 0x88, 0x88)).setLineWidth(0.75f)
						.moveTo(borderLeft, nameRowBottom).lineTo(borderRight, nameRowBottom).stroke().restoreState();

				float textY = nameRowBottom + 7f;
				float nameX = borderLeft + 10f;
				float datumX = borderLeft + (borderRight - borderLeft) * 0.55f;

				canvas.setFillColor(new DeviceRgb(0x22, 0x22, 0x22));

				canvas.beginText().setFontAndSize(boldFont, 10f).moveText(nameX, textY).showText("Name:").endText()
						.beginText().setFontAndSize(regularFont, 10f)
						.moveText(nameX + boldFont.getWidth("Name:", 10f) + 4f, textY)
						.showText("________________________________").endText().beginText()
						.setFontAndSize(boldFont, 10f).moveText(datumX, textY).showText("Datum:").endText().beginText()
						.setFontAndSize(regularFont, 10f)
						.moveText(datumX + boldFont.getWidth("Datum:", 10f) + 4f, textY).showText("________________")
						.endText();
			}

			canvas.release();
		}
	}
}
