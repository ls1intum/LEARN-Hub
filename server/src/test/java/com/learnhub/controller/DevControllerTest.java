package com.learnhub.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.learnhub.documentmanagement.dto.response.TestMarkdownResponse;
import com.learnhub.documentmanagement.service.LLMService;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Verifies the dev-only markdown generation pipeline end-to-end with the LLM
 * mocked out: a real PDF is uploaded and parsed, then each generator returns
 * deterministic dummy markdown. No external model is ever contacted.
 */
class DevControllerTest {

	private DevController controller;
	private StubLLMService llmService;

	@BeforeEach
	void setUp() {
		controller = new DevController();
		llmService = new StubLLMService();
		ReflectionTestUtils.setField(controller, "llmService", llmService);
		ReflectionTestUtils.setField(controller, "visionMaxPages", 5);
		ReflectionTestUtils.setField(controller, "visionDpi", 72);
	}

	private MockMultipartFile pdfUpload() throws Exception {
		try (PDDocument doc = new PDDocument()) {
			doc.addPage(new PDPage());
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			doc.save(baos);
			return new MockMultipartFile("file", "exercise.pdf", "application/pdf", baos.toByteArray());
		}
	}

	@Test
	void testMarkdownGeneratesUebungAndSolution() throws Exception {
		ResponseEntity<?> response = controller.testMarkdown(pdfUpload(), "uebung", null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		TestMarkdownResponse body = (TestMarkdownResponse) response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.getUebungMarkdown()).isEqualTo("# Dummy Uebung");
		assertThat(body.getUebungLoesungMarkdown()).isEqualTo("# Dummy Loesung");
		assertThat(llmService.uebungCalls).isEqualTo(1);
	}

	@Test
	void testMarkdownGeneratesDeckblatt() throws Exception {
		ResponseEntity<?> response = controller.testMarkdown(pdfUpload(), "deckblatt", null);

		TestMarkdownResponse body = (TestMarkdownResponse) response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.getDeckblattMarkdown()).isEqualTo("# Dummy Deckblatt");
	}

	@Test
	void testMarkdownGeneratesHintergrundwissen() throws Exception {
		ResponseEntity<?> response = controller.testMarkdown(pdfUpload(), "hintergrundwissen", null);

		TestMarkdownResponse body = (TestMarkdownResponse) response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.getHintergrundwissenMarkdown()).isEqualTo("# Dummy Hintergrundwissen");
	}

	@Test
	void testMarkdownGeneratesArtikulationsschema() throws Exception {
		ResponseEntity<?> response = controller.testMarkdown(pdfUpload(), "artikulationsschema", null);

		TestMarkdownResponse body = (TestMarkdownResponse) response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.getArtikulationsschemaMarkdown()).isEqualTo("# Dummy Artikulationsschema");
	}

	@Test
	void testMarkdownGeneratesTafelbildFromArtikulationsschema() throws Exception {
		ResponseEntity<?> response = controller.testMarkdown(pdfUpload(), "tafelbild", null);

		TestMarkdownResponse body = (TestMarkdownResponse) response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.getArtikulationsschemaMarkdown()).isEqualTo("# Dummy Artikulationsschema");
		assertThat(body.getTafelbildMarkdown()).isEqualTo("# Dummy Tafelbild");
	}

	@Test
	void testMarkdownAcceptsMetadataJson() throws Exception {
		ResponseEntity<?> response = controller.testMarkdown(pdfUpload(), "deckblatt",
				"{\"name\":\"Binary Bracelets\"}");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(llmService.lastMetadata).containsEntry("name", "Binary Bracelets");
	}

	@Test
	void testMarkdownRejectsUnknownType() throws Exception {
		ResponseEntity<?> response = controller.testMarkdown(pdfUpload(), "bogus", null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	private static final class StubLLMService extends LLMService {

		private int uebungCalls;
		private Map<String, Object> lastMetadata;

		private StubLLMService() {
			super(unsupportedChatClient());
		}

		@Override
		public boolean isVisionEnabled() {
			return false;
		}

		@Override
		public Map<String, String> generateUebungAndLoesung(String pdfText, Map<String, Object> metadata,
				java.util.List<byte[]> images) {
			uebungCalls++;
			lastMetadata = metadata;
			return Map.of("uebung", "# Dummy Uebung", "uebung_loesung", "# Dummy Loesung");
		}

		@Override
		public String generateDeckblatt(String pdfText, Map<String, Object> metadata) {
			lastMetadata = metadata;
			return "# Dummy Deckblatt";
		}

		@Override
		public String generateHintergrundwissen(String pdfText, Map<String, Object> metadata) {
			lastMetadata = metadata;
			return "# Dummy Hintergrundwissen";
		}

		@Override
		public String generateArtikulationsschema(String pdfText, Map<String, Object> metadata) {
			lastMetadata = metadata;
			return "# Dummy Artikulationsschema";
		}

		@Override
		public String generateTafelbildMarkdown(String artikulationsschema, Map<String, Object> metadata) {
			lastMetadata = metadata;
			return "# Dummy Tafelbild";
		}

		private static ChatClient unsupportedChatClient() {
			return (ChatClient) Proxy.newProxyInstance(ChatClient.class.getClassLoader(), new Class[]{ChatClient.class},
					(proxy, method, args) -> {
						throw new UnsupportedOperationException("ChatClient should not be called in dev tests");
					});
		}
	}
}
