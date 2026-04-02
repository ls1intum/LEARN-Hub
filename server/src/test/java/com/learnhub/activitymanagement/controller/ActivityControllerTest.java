package com.learnhub.activitymanagement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.learnhub.activitymanagement.dto.request.GenerateMarkdownsRequest;
import com.learnhub.activitymanagement.dto.response.GenerateMarkdownsResponse;
import com.learnhub.documentmanagement.service.LLMService;
import com.learnhub.documentmanagement.service.MarkdownToDocxService;
import com.learnhub.documentmanagement.service.MarkdownToPdfService;
import com.learnhub.documentmanagement.service.PDFService;
import com.learnhub.service.SanitizationService;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

class ActivityControllerTest {

	private ActivityController activityController;
	private StubPdfService pdfService;
	private StubLLMService llmService;

	@BeforeEach
	void setUp() {
		activityController = new ActivityController();
		pdfService = new StubPdfService();
		llmService = new StubLLMService();

		ReflectionTestUtils.setField(activityController, "pdfService", pdfService);
		ReflectionTestUtils.setField(activityController, "llmService", llmService);
		ReflectionTestUtils.setField(activityController, "markdownGenerationExecutor", (Executor) Runnable::run);

		var markdownToHtmlService = new com.learnhub.documentmanagement.service.MarkdownToHtmlService();
		ReflectionTestUtils.setField(markdownToHtmlService, "sanitizationService", new SanitizationService());
		ReflectionTestUtils.setField(activityController, "markdownToPdfService",
				new MarkdownToPdfService(markdownToHtmlService));
		ReflectionTestUtils.setField(activityController, "markdownToDocxService",
				new MarkdownToDocxService(markdownToHtmlService, null, null));
	}

	@Test
	void generateActivityMarkdownsRunsAllRequestedGenerators() {
		UUID documentId = UUID.randomUUID();
		Map<String, Object> metadata = Map.of("name", "Binary Bracelets");
		GenerateMarkdownsRequest request = new GenerateMarkdownsRequest();
		request.setDocumentId(documentId);
		request.setMetadata(metadata);

		pdfService.documentId = documentId;
		pdfService.pdfText = "PDF text with enough content";
		llmService.deckblatt = "# Deckblatt";
		llmService.artikulationsschema = "# Artikulationsschema";
		llmService.hintergrundwissen = "# Hintergrundwissen";
		llmService.uebung = "# Uebung";
		llmService.uebungLoesung = "# Loesung";

		ResponseEntity<GenerateMarkdownsResponse> response = activityController.generateActivityMarkdowns(request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getDocumentId()).isEqualTo(documentId.toString());
		assertThat(response.getBody().getDeckblattMarkdown()).isEqualTo("# Deckblatt");
		assertThat(response.getBody().getArtikulationsschemaMarkdown()).isEqualTo("# Artikulationsschema");
		assertThat(response.getBody().getHintergrundwissenMarkdown()).isEqualTo("# Hintergrundwissen");
		assertThat(response.getBody().getUebungMarkdown()).isEqualTo("# Uebung");
		assertThat(response.getBody().getUebungLoesungMarkdown()).isEqualTo("# Loesung");
		assertThat(llmService.deckblattCalls).isEqualTo(1);
		assertThat(llmService.artikulationsschemaCalls).isEqualTo(1);
		assertThat(llmService.hintergrundwissenCalls).isEqualTo(1);
		assertThat(llmService.uebungCalls).isEqualTo(1);
	}

	@Test
	void generateActivityMarkdownsOnlyRunsRequestedTypes() {
		UUID documentId = UUID.randomUUID();
		Map<String, Object> metadata = Map.of("name", "Binary Bracelets");
		GenerateMarkdownsRequest request = new GenerateMarkdownsRequest();
		request.setDocumentId(documentId);
		request.setMetadata(metadata);
		request.setTypes(List.of("deckblatt", "uebung_loesung"));

		pdfService.documentId = documentId;
		pdfService.pdfText = "PDF text with enough content";
		llmService.deckblatt = "# Deckblatt";
		llmService.uebung = "# Uebung";
		llmService.uebungLoesung = "# Loesung";

		ResponseEntity<GenerateMarkdownsResponse> response = activityController.generateActivityMarkdowns(request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getDeckblattMarkdown()).isEqualTo("# Deckblatt");
		assertThat(response.getBody().getArtikulationsschemaMarkdown()).isNull();
		assertThat(response.getBody().getHintergrundwissenMarkdown()).isNull();
		assertThat(response.getBody().getUebungMarkdown()).isEqualTo("# Uebung");
		assertThat(response.getBody().getUebungLoesungMarkdown()).isEqualTo("# Loesung");
		assertThat(llmService.deckblattCalls).isEqualTo(1);
		assertThat(llmService.artikulationsschemaCalls).isZero();
		assertThat(llmService.hintergrundwissenCalls).isZero();
		assertThat(llmService.uebungCalls).isEqualTo(1);
	}

	@Test
	void generateActivityMarkdownsPropagatesGeneratorFailures() {
		UUID documentId = UUID.randomUUID();
		GenerateMarkdownsRequest request = new GenerateMarkdownsRequest();
		request.setDocumentId(documentId);
		request.setMetadata(Map.of("name", "Binary Bracelets"));
		request.setTypes(List.of("deckblatt"));

		pdfService.documentId = documentId;
		pdfService.pdfText = "PDF text with enough content";
		llmService.deckblattFailure = new RuntimeException("LLM unavailable");

		assertThatThrownBy(() -> activityController.generateActivityMarkdowns(request))
				.isInstanceOf(RuntimeException.class).hasMessageContaining("LLM unavailable");
	}

	private static final class StubPdfService extends PDFService {

		private UUID documentId;
		private String pdfText;

		@Override
		public String extractTextFromPdf(UUID requestedDocumentId) {
			assertThat(requestedDocumentId).isEqualTo(documentId);
			return pdfText;
		}
	}

	private static final class StubLLMService extends LLMService {

		private String deckblatt;
		private String artikulationsschema;
		private String hintergrundwissen;
		private String uebung;
		private String uebungLoesung;
		private RuntimeException deckblattFailure;
		private int deckblattCalls;
		private int artikulationsschemaCalls;
		private int hintergrundwissenCalls;
		private int uebungCalls;

		private StubLLMService() {
			super(unsupportedChatClient());
		}

		@Override
		public String generateDeckblatt(String pdfText, Map<String, Object> metadata) {
			deckblattCalls++;
			if (deckblattFailure != null) {
				throw deckblattFailure;
			}
			return deckblatt;
		}

		@Override
		public String generateArtikulationsschema(String pdfText, Map<String, Object> metadata) {
			artikulationsschemaCalls++;
			return artikulationsschema;
		}

		@Override
		public String generateHintergrundwissen(String pdfText, Map<String, Object> metadata) {
			hintergrundwissenCalls++;
			return hintergrundwissen;
		}

		@Override
		public Map<String, String> generateUebungAndLoesung(String pdfText, Map<String, Object> metadata) {
			uebungCalls++;
			return Map.of("uebung", uebung, "uebung_loesung", uebungLoesung);
		}

		private static ChatClient unsupportedChatClient() {
			return (ChatClient) Proxy.newProxyInstance(ChatClient.class.getClassLoader(), new Class[]{ChatClient.class},
					(proxy, method, args) -> {
						throw new UnsupportedOperationException("ChatClient should not be called in controller tests");
					});
		}
	}
}
