package com.learnhub.documentmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

class DocxPostProcessorTest {

	private static final byte[] PNG_BYTES = Base64.getDecoder()
			.decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aX7wAAAAASUVORK5CYII=");

	@Test
	void processAddsHeaderFooterAndOrientation() throws Exception {
		// Create a minimal DOCX
		byte[] input;
		try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			doc.createParagraph().createRun().setText("test content");
			doc.write(out);
			input = out.toByteArray();
		}

		DocxPostProcessor processor = new DocxPostProcessor();
		byte[] result = processor.process(input, true, "My Activity");

		try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(result))) {
			// Check orientation
			CTSectPr sectPr = doc.getDocument().getBody().getSectPr();
			assertThat(sectPr).isNotNull();
			assertThat(sectPr.getPgSz().getOrient().toString()).isEqualTo("landscape");
			assertThat(doc.getHeaderList()).isNotEmpty();
			assertThat(doc.getFooterList()).isNotEmpty();
		}
	}

	@Test
	void processSetsComicSansOnBodyRuns() throws Exception {
		byte[] input;
		try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			doc.createParagraph().createRun().setText("test content");
			doc.write(out);
			input = out.toByteArray();
		}

		byte[] result = new DocxPostProcessor().process(input, true, "My Activity");

		try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(result))) {
			assertThat(doc.getParagraphArray(0).getRuns().get(0).getFontFamily()).isEqualTo("Comic Sans MS");
		}
	}

	@Test
	void processShrinksOversizedLandscapeImagesToPdfLikeCap() throws Exception {
		byte[] input;
		try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			XWPFParagraph paragraph = doc.createParagraph();
			paragraph.createRun().addPicture(new ByteArrayInputStream(PNG_BYTES), XWPFDocument.PICTURE_TYPE_PNG, "test.png",
					Units.toEMU(500), Units.toEMU(500));
			doc.write(out);
			input = out.toByteArray();
		}

		byte[] result = new DocxPostProcessor().process(input, true, "My Activity");

		try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(result))) {
			CTDrawing drawing = doc.getParagraphArray(0).getRuns().get(0).getCTR().getDrawingArray(0);
			assertThat(drawing.getInlineArray(0).getExtent().getCy()).isLessThanOrEqualTo(43L * 36000L);
		}
	}
}
