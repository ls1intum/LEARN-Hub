package com.learnhub.documentmanagement.service;

import com.learnhub.service.SanitizationService;
import java.io.FileOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.test.util.ReflectionTestUtils;

class FullPipelineDebugTest {

	static boolean libreOfficeAvailable() {
		try {
			Process p = new ProcessBuilder("/Applications/LibreOffice.app/Contents/MacOS/soffice", "--version").start();
			return p.waitFor() == 0;
		} catch (Exception e) {
			return false;
		}
	}

	@Test
	@EnabledIf("libreOfficeAvailable")
	void fullPipelineSingleDocx() throws Exception {
		LibreOfficeConversionService conv = new LibreOfficeConversionService();
		setField(conv, "libreofficePath", "/Applications/LibreOffice.app/Contents/MacOS/soffice");
		setField(conv, "timeoutSeconds", 60);

		MarkdownToHtmlService htmlService = new MarkdownToHtmlService();
		ReflectionTestUtils.setField(htmlService, "sanitizationService", new SanitizationService());
		DocxPostProcessor postProcessor = new DocxPostProcessor();
		MarkdownToDocxService docxService = new MarkdownToDocxService(htmlService, conv, postProcessor);

		String markdown = """
				# Test Title

				Some content here with **bold** and *italic*.

				| Zeit | Phase | Inhalt | Sozialform |
				|------|-------|--------|------------|
				| 5 min | Einstieg | Begrüßung | Plenum |
				| 20 min | Erarbeitung | Aufgaben | Einzelarbeit |
				| 10 min | Sicherung | Ergebnisse | Plenum |
				""";

		byte[] result = docxService.renderMarkdownToDocx(markdown, true, "My Activity");
		try (FileOutputStream fos = new FileOutputStream("/tmp/debug-final.docx")) {
			fos.write(result);
		}
		System.out.println("Saved /tmp/debug-final.docx");
	}

	private void setField(Object obj, String name, Object value) throws Exception {
		var field = obj.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(obj, value);
	}
}
