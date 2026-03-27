package com.learnhub.documentmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarkdownToDocxServiceTest {

	private MarkdownToDocxService service;
	private StubConversionService stubConversionService;

	@BeforeEach
	void setUp() {
		stubConversionService = new StubConversionService(createMinimalDocx());
		service = new MarkdownToDocxService(new MarkdownToHtmlService(), stubConversionService,
				new DocxPostProcessor());
	}

	@Test
	void renderMarkdownToDocxDelegatesToHtmlThenLibreOffice() {
		byte[] result = service.renderMarkdownToDocx("# Hello", true, "Activity");

		assertThat(result).isNotNull();
		assertThat(result[0]).isEqualTo((byte) 'P');
		assertThat(stubConversionService.lastInput).isNotNull();
		assertThat(new String(stubConversionService.lastInput)).contains("Hello</h1>");
	}

	@Test
	void renderMarkdownToDocxDefaultsToLandscape() {
		service.renderMarkdownToDocx("text");

		String html = new String(stubConversionService.lastInput);
		assertThat(html).contains("landscape");
	}

	@Test
	void renderMarkdownToDocxAddsHeaderAndFooter() throws Exception {
		byte[] result = service.renderMarkdownToDocx("# Test", true, "My Activity");

		try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(result))) {
			assertThat(doc.getHeaderList()).isNotEmpty();
			assertThat(doc.getFooterList()).isNotEmpty();
			assertThat(doc.getHeaderList().get(0).getText()).contains("My Activity");
		}
	}

	@Test
	void renderMarkdownToDocxSetsOrientation() throws Exception {
		byte[] result = service.renderMarkdownToDocx("# Test", false, "");

		try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(result))) {
			var sectPr = doc.getDocument().getBody().getSectPr();
			assertThat(sectPr.getPgSz().getOrient().toString()).isEqualTo("portrait");
		}
	}

	@Test
	void renderHtmlToDocxDelegatesToLibreOffice() {
		byte[] result = service.renderHtmlToDocx("<h1>Test</h1>", true, "Act");

		assertThat(result).isNotNull();
		assertThat(new String(stubConversionService.lastInput)).contains("Test</h1>");
	}

	@Test
	void renderMergedDocxConvertsEachSectionIndividually() {
		byte[] result = service.renderMergedDocx(List.of("# A", "# B"), List.of(false, true), "Test");

		assertThat(result).isNotNull();
		// Each section is converted individually, so the stub should have been called
		// twice
		assertThat(stubConversionService.callCount).isEqualTo(2);
	}

	@Test
	void renderMergedDocxRejectsMismatchedLists() {
		assertThatThrownBy(() -> service.renderMergedDocx(List.of("# A", "# B"), List.of(false), "Test"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void renderMarkdownToDocxWrapsConversionException() {
		stubConversionService.failWith = new IOException("conversion failed");

		assertThatThrownBy(() -> service.renderMarkdownToDocx("text")).isInstanceOf(RuntimeException.class)
				.hasMessageContaining("conversion failed");
	}

	private static byte[] createMinimalDocx() {
		try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			doc.createParagraph().createRun().setText("stub");
			doc.write(out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static class StubConversionService extends LibreOfficeConversionService {

		private final byte[] output;
		byte[] lastInput;
		int callCount;
		IOException failWith;

		StubConversionService(byte[] output) {
			this.output = output;
		}

		@Override
		public byte[] convertHtmlToDocx(byte[] htmlBytes) throws IOException {
			if (failWith != null) {
				throw failWith;
			}
			this.lastInput = htmlBytes;
			this.callCount++;
			return output;
		}
	}
}
