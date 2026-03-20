package com.learnhub.documentmanagement.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.learnhub.activitymanagement.entity.Activity;
import com.learnhub.activitymanagement.entity.ActivityMarkdown;
import com.learnhub.activitymanagement.entity.enums.MarkdownType;
import com.learnhub.activitymanagement.repository.ActivityMarkdownRepository;
import com.learnhub.documentmanagement.service.DocxHeaderFooterHelper;
import com.learnhub.documentmanagement.service.DocxTableHelper;
import com.learnhub.documentmanagement.service.MarkdownToDocxService;
import com.learnhub.documentmanagement.service.MarkdownToHtmlService;
import com.learnhub.documentmanagement.service.MarkdownToPdfService;
import java.lang.reflect.Proxy;
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
		ReflectionTestUtils.setField(markdownController, "markdownToPdfService",
				new MarkdownToPdfService(markdownToHtmlService));
		ReflectionTestUtils.setField(markdownController, "markdownToDocxService",
				new MarkdownToDocxService(markdownToHtmlService, new DocxHeaderFooterHelper(),
						new DocxTableHelper()));
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
