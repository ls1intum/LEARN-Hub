package com.learnhub.documentmanagement.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.learnhub.activitymanagement.entity.Activity;
import com.learnhub.activitymanagement.entity.ActivityMarkdown;
import com.learnhub.activitymanagement.entity.enums.MarkdownType;
import com.learnhub.activitymanagement.repository.ActivityMarkdownRepository;
import com.learnhub.documentmanagement.dto.request.MarkdownPreviewRequest;
import com.learnhub.documentmanagement.service.AdobePdfToDocxService;
import com.learnhub.documentmanagement.service.MarkdownToDocxService;
import com.learnhub.documentmanagement.service.MarkdownToHtmlService;
import com.learnhub.documentmanagement.service.MarkdownToPdfService;
import com.learnhub.service.SanitizationService;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

class MarkdownControllerTest {

	private MarkdownController markdownController;

	@BeforeEach
	void setUp() {
		markdownController = new MarkdownController();
		MarkdownToHtmlService markdownToHtmlService = new MarkdownToHtmlService();
		ReflectionTestUtils.setField(markdownToHtmlService, "sanitizationService", new SanitizationService());
		MarkdownToPdfService pdfService = new MarkdownToPdfService(markdownToHtmlService);
		ReflectionTestUtils.setField(markdownController, "markdownToPdfService", pdfService);
		// DOCX service is not used by the PDF endpoint tests — provide a no-op instance
		ReflectionTestUtils.setField(markdownController, "markdownToDocxService",
				new MarkdownToDocxService(pdfService, null));
	}

	@Test
	void getMarkdownPdfFetchesActivityNameWithRepositoryMethodThatLoadsActivity() {
		UUID markdownId = UUID.randomUUID();
		Activity activity = new Activity();
		activity.setName("Binary Search Game");

		ActivityMarkdown markdown = new ActivityMarkdown();
		markdown.setId(markdownId);
		markdown.setType(MarkdownType.ARTIKULATIONSSCHEMA);
		markdown.setContent("# Heading");
		markdown.setLandscape(true);
		markdown.setActivity(activity);

		ReflectionTestUtils.setField(markdownController, "markdownRepository",
				repositoryReturning(Optional.of(markdown), markdownId));

		ResponseEntity<?> response = markdownController.getMarkdownPdf(markdownId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isInstanceOf(byte[].class);
		assertThat(((byte[]) response.getBody()).length).isGreaterThan(0);
	}

	@Test
	void getMarkdownPdfReturnsNotFoundWhenMarkdownDoesNotExist() {
		UUID markdownId = UUID.randomUUID();
		ReflectionTestUtils.setField(markdownController, "markdownRepository",
				repositoryReturning(Optional.empty(), markdownId));

		ResponseEntity<?> response = markdownController.getMarkdownPdf(markdownId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void getMarkdownPdfReturnsNotFoundWhenContentEmpty() {
		UUID markdownId = UUID.randomUUID();
		ActivityMarkdown markdown = new ActivityMarkdown();
		markdown.setId(markdownId);
		markdown.setType(MarkdownType.DECKBLATT);
		markdown.setContent("   ");
		ReflectionTestUtils.setField(markdownController, "markdownRepository",
				repositoryReturning(Optional.of(markdown), markdownId));

		ResponseEntity<?> response = markdownController.getMarkdownPdf(markdownId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void getCapabilitiesReportsDocxUnavailableWhenAdobeNotConfigured() {
		useDocxServiceWithAdobeConfigured(false);

		ResponseEntity<Map<String, Boolean>> response = markdownController.getCapabilities();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).containsEntry("docxAvailable", false);
	}

	@Test
	void getMarkdownDocxReturns503WhenDocxUnavailable() {
		useDocxServiceWithAdobeConfigured(false);

		ResponseEntity<?> response = markdownController.getMarkdownDocx(UUID.randomUUID());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	@Test
	void previewMarkdownPdfRejectsEmptyMarkdown() {
		MarkdownPreviewRequest request = new MarkdownPreviewRequest();
		request.setMarkdown("   ");

		ResponseEntity<?> response = markdownController.previewMarkdownPdf(request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void previewMarkdownPdfRendersPdfBytes() {
		MarkdownPreviewRequest request = new MarkdownPreviewRequest();
		request.setMarkdown("# Vorschau\n\nEin Absatz.");
		request.setActivityName("Binary Search Game");

		ResponseEntity<?> response = markdownController.previewMarkdownPdf(request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isInstanceOf(byte[].class);
		assertThat(((byte[]) response.getBody()).length).isGreaterThan(0);
	}

	private void useDocxServiceWithAdobeConfigured(boolean configured) {
		MarkdownToHtmlService htmlService = new MarkdownToHtmlService();
		ReflectionTestUtils.setField(htmlService, "sanitizationService", new SanitizationService());
		MarkdownToPdfService pdfService = new MarkdownToPdfService(htmlService);
		ReflectionTestUtils.setField(markdownController, "markdownToDocxService",
				new MarkdownToDocxService(pdfService, new StubAdobeService(configured)));
	}

	private static final class StubAdobeService extends AdobePdfToDocxService {

		private final boolean configured;

		private StubAdobeService(boolean configured) {
			this.configured = configured;
		}

		@Override
		public boolean isConfigured() {
			return configured;
		}
	}

	private ActivityMarkdownRepository repositoryReturning(Optional<ActivityMarkdown> result, UUID expectedMarkdownId) {
		return (ActivityMarkdownRepository) Proxy.newProxyInstance(ActivityMarkdownRepository.class.getClassLoader(),
				new Class[]{ActivityMarkdownRepository.class}, (proxy, method, args) -> {
					if ("findWithActivityById".equals(method.getName())) {
						assertThat(args).containsExactly(expectedMarkdownId);
						return result;
					}

					if ("equals".equals(method.getName())) {
						return proxy == args[0];
					}
					if ("hashCode".equals(method.getName())) {
						return System.identityHashCode(proxy);
					}
					if ("toString".equals(method.getName())) {
						return "ActivityMarkdownRepositoryStub";
					}

					throw new UnsupportedOperationException("Unexpected repository method: " + method.getName());
				});
	}
}
