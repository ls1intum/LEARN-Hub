package com.learnhub.documentmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarkdownToDocxServiceTest {

	private MarkdownToDocxService service;
	private StubAdobeService stubAdobeService;
	private StubPdfService stubPdfService;

	@BeforeEach
	void setUp() {
		stubAdobeService = new StubAdobeService(new byte[] { 0x50, 0x4B }); // PK zip header
		stubPdfService = new StubPdfService();
		service = new MarkdownToDocxService(stubPdfService, stubAdobeService);
	}

	@Test
	void renderMarkdownToDocxDelegatesToPdfThenAdobe() {
		byte[] result = service.renderMarkdownToDocx("# Hello", true, "Activity");

		assertThat(result).isNotNull();
		assertThat(stubAdobeService.callCount).isEqualTo(1);
		assertThat(stubPdfService.lastMarkdown).isEqualTo("# Hello");
	}

	@Test
	void renderMarkdownToDocxDefaultsToLandscape() {
		service.renderMarkdownToDocx("text");

		assertThat(stubPdfService.lastLandscape).isTrue();
	}

	@Test
	void renderHtmlToDocxDelegatesToPdfThenAdobe() {
		byte[] result = service.renderHtmlToDocx("<h1>Test</h1>", true, "Act");

		assertThat(result).isNotNull();
		assertThat(stubAdobeService.callCount).isEqualTo(1);
		assertThat(stubPdfService.lastHtml).contains("Test");
	}

	@Test
	void renderMergedDocxConvertsEachSectionThenMerges() {
		byte[] result = service.renderMergedDocx(
				List.of("# A", "# B"), List.of(false, true), List.of(false, false), "Test");

		assertThat(result).isNotNull();
		assertThat(stubPdfService.pdfCallCount).isEqualTo(2);
		assertThat(stubAdobeService.callCount).isEqualTo(1);
	}

	@Test
	void renderMergedDocxRejectsMismatchedLists() {
		assertThatThrownBy(
				() -> service.renderMergedDocx(List.of("# A", "# B"), List.of(false), List.of(false, false), "Test"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void renderMarkdownToDocxWrapsConversionException() {
		stubAdobeService.failWith = new RuntimeException("conversion failed");

		assertThatThrownBy(() -> service.renderMarkdownToDocx("text")).isInstanceOf(RuntimeException.class)
				.hasMessageContaining("conversion failed");
	}

	@Test
	void isAvailableDelegatesToAdobeService() {
		assertThat(service.isAvailable()).isFalse();

		stubAdobeService.configured = true;
		assertThat(service.isAvailable()).isTrue();
	}

	static class StubAdobeService extends AdobePdfToDocxService {

		private final byte[] output;
		int callCount;
		RuntimeException failWith;
		boolean configured = false;

		StubAdobeService(byte[] output) {
			this.output = output;
		}

		@Override
		public boolean isConfigured() {
			return configured;
		}

		@Override
		public byte[] convertPdfToDocx(byte[] pdfBytes) {
			if (failWith != null) {
				throw failWith;
			}
			callCount++;
			return output;
		}
	}

	static class StubPdfService extends MarkdownToPdfService {

		String lastMarkdown;
		boolean lastLandscape;
		String lastHtml;
		int pdfCallCount;

		StubPdfService() {
			super(null);
		}

		@Override
		public byte[] renderMarkdownToPdf(String markdown, boolean landscape, String activityName,
				boolean exerciseSheet) {
			this.lastMarkdown = markdown;
			this.lastLandscape = landscape;
			pdfCallCount++;
			return new byte[] { 0x25, 0x50, 0x44, 0x46 }; // %PDF
		}

		@Override
		public byte[] renderHtmlToPdf(String htmlBody, boolean landscape, String activityName) {
			this.lastHtml = htmlBody;
			pdfCallCount++;
			return new byte[] { 0x25, 0x50, 0x44, 0x46 }; // %PDF
		}

		@Override
		public byte[] mergePdfs(List<byte[]> pdfs) {
			return new byte[] { 0x25, 0x50, 0x44, 0x46 }; // %PDF
		}
	}
}
