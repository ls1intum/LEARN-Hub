package com.learnhub.activitymanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnhub.activitymanagement.dto.response.ActivityResponse;
import com.learnhub.activitymanagement.dto.response.MarkdownResponse;
import com.learnhub.activitymanagement.entity.Activity;
import com.learnhub.activitymanagement.entity.ActivityMarkdown;
import com.learnhub.activitymanagement.entity.enums.ActivityFormat;
import com.learnhub.activitymanagement.entity.enums.BloomLevel;
import com.learnhub.activitymanagement.entity.enums.DocumentType;
import com.learnhub.activitymanagement.entity.enums.MarkdownType;
import com.learnhub.activitymanagement.repository.ActivityRepository;
import com.learnhub.documentmanagement.entity.PDFDocument;
import com.learnhub.documentmanagement.repository.PDFDocumentRepository;
import com.learnhub.documentmanagement.service.LLMService;
import com.learnhub.documentmanagement.service.PDFService;
import com.learnhub.service.SanitizationService;
import com.learnhub.usermanagement.entity.UserFavourites;
import com.learnhub.usermanagement.repository.UserFavouritesRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class ActivityServiceTest {

	private ActivityService activityService;
	private ActivityExtractionService extractionService;
	private ActivityRepository activityRepository;
	private PDFDocumentRepository pdfDocumentRepository;
	private PDFService pdfService;
	private LLMService llmService;
	private UserFavouritesRepository userFavouritesRepository;

	@BeforeEach
	void setUp() {
		activityService = new ActivityService();
		extractionService = new ActivityExtractionService();
		activityRepository = mock(ActivityRepository.class);
		pdfDocumentRepository = mock(PDFDocumentRepository.class);
		pdfService = mock(PDFService.class);
		llmService = mock(LLMService.class);
		userFavouritesRepository = mock(UserFavouritesRepository.class);

		ReflectionTestUtils.setField(activityService, "activityRepository", activityRepository);
		ReflectionTestUtils.setField(activityService, "pdfDocumentRepository", pdfDocumentRepository);
		ReflectionTestUtils.setField(activityService, "pdfService", pdfService);
		ReflectionTestUtils.setField(activityService, "extractionService", extractionService);
		ReflectionTestUtils.setField(activityService, "sanitizationService", new SanitizationService());
		ReflectionTestUtils.setField(activityService, "userFavouritesRepository", userFavouritesRepository);
		ReflectionTestUtils.setField(extractionService, "pdfService", pdfService);
		ReflectionTestUtils.setField(extractionService, "llmService", llmService);
	}

	@Test
	void createActivityFromMapCreatesDocumentRelationship() {
		UUID docId = UUID.randomUUID();
		PDFDocument doc = createTestDocument(docId);
		when(pdfDocumentRepository.findById(docId)).thenReturn(Optional.of(doc));
		when(pdfDocumentRepository.save(any(PDFDocument.class))).thenAnswer(inv -> inv.getArgument(0));

		Map<String, Object> data = new HashMap<>();
		data.put("name", "Test Activity");
		data.put("description", "A test activity description");
		data.put("ageMin", 8);
		data.put("ageMax", 12);
		data.put("format", "unplugged");
		data.put("bloomLevel", "apply");
		data.put("durationMinMinutes", 30);
		data.put("documentId", docId.toString());

		Activity activity = activityService.createActivityFromMap(data);

		assertThat(activity.getDocuments()).hasSize(1);
		PDFDocument actDoc = activity.getDocuments().get(0);
		assertThat(actDoc.getId()).isEqualTo(docId);
		assertThat(actDoc.getType()).isEqualTo(DocumentType.SOURCE_PDF);
	}

	@Test
	void createActivityFromMapCreatesMarkdownRelationship() {
		Map<String, Object> data = new HashMap<>();
		data.put("name", "Test Activity");
		data.put("description", "A test activity description");
		data.put("ageMin", 8);
		data.put("ageMax", 12);
		data.put("format", "unplugged");
		data.put("bloomLevel", "apply");
		data.put("durationMinMinutes", 30);
		data.put("artikulationsschemaMarkdown", "# Schema\n\nSome markdown content");

		Activity activity = activityService.createActivityFromMap(data);

		assertThat(activity.getMarkdowns()).hasSize(1);
		ActivityMarkdown actMd = activity.getMarkdowns().get(0);
		assertThat(actMd.getContent()).isEqualTo("# Schema\n\nSome markdown content");
		assertThat(actMd.getType()).isEqualTo(MarkdownType.ARTIKULATIONSSCHEMA);
		assertThat(actMd.getActivity()).isSameAs(activity);
	}

	@Test
	void createActivityFromMapCreatesNeitherWhenNotProvided() {
		Map<String, Object> data = new HashMap<>();
		data.put("name", "Test Activity");
		data.put("description", "A test activity description");
		data.put("ageMin", 8);
		data.put("ageMax", 12);
		data.put("format", "unplugged");
		data.put("bloomLevel", "apply");
		data.put("durationMinMinutes", 30);

		Activity activity = activityService.createActivityFromMap(data);

		assertThat(activity.getDocuments()).isEmpty();
		assertThat(activity.getMarkdowns()).isEmpty();
	}

	@Test
	void createActivityFromMapThrowsOnInvalidDocumentId() {
		Map<String, Object> data = new HashMap<>();
		data.put("name", "Test Activity");
		data.put("description", "A test activity description");
		data.put("ageMin", 8);
		data.put("ageMax", 12);
		data.put("format", "unplugged");
		data.put("bloomLevel", "apply");
		data.put("durationMinMinutes", 30);
		data.put("documentId", "not-a-uuid");

		assertThatThrownBy(() -> activityService.createActivityFromMap(data))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid documentId format");
	}

	@Test
	void deleteActivityRemovesActivityFavouritesBeforeDeletingActivity() {
		UUID activityId = UUID.randomUUID();

		activityService.deleteActivity(activityId);

		InOrder inOrder = org.mockito.Mockito.inOrder(userFavouritesRepository, activityRepository);
		inOrder.verify(userFavouritesRepository).deleteByActivityId(activityId);
		inOrder.verify(activityRepository).deleteById(activityId);
	}

	@Test
	void mapToResponseOmitsSourcePdfDocuments() {
		Activity activity = createTestActivity();
		UUID docId = UUID.randomUUID();

		PDFDocument doc = createTestDocument(docId);
		activity.getDocuments().add(doc);

		ActivityResponse response = activityService.convertToResponse(activity);

		assertThat(response.getDocuments()).isEmpty();
	}

	@Test
	void mapToResponseIncludesSourcePdfDocumentsForAdmin() {
		Activity activity = createTestActivity();
		UUID docId = UUID.randomUUID();

		PDFDocument doc = createTestDocument(docId);
		activity.getDocuments().add(doc);

		ActivityResponse response = activityService.convertToResponse(activity, true);

		assertThat(response.getDocuments()).hasSize(1);
		assertThat(response.getDocuments().get(0).getId()).isEqualTo(docId);
		assertThat(response.getDocuments().get(0).getType()).isEqualTo("source_pdf");
	}

	@Test
	void mapToResponseReturnsMarkdownsList() {
		Activity activity = createTestActivity();

		ActivityMarkdown actMd = new ActivityMarkdown();
		actMd.setId(UUID.randomUUID());
		actMd.setActivity(activity);
		actMd.setType(MarkdownType.ARTIKULATIONSSCHEMA);
		actMd.setContent("# Test Schema");
		actMd.setCreatedAt(LocalDateTime.now());
		activity.getMarkdowns().add(actMd);

		ActivityResponse response = activityService.convertToResponse(activity);

		assertThat(response.getMarkdowns()).hasSize(1);
		MarkdownResponse mdResp = response.getMarkdowns().get(0);
		assertThat(mdResp.getId()).isEqualTo(actMd.getId());
		assertThat(mdResp.getType()).isEqualTo("artikulationsschema");
		assertThat(mdResp.getContent()).isNull();
	}

	@Test
	void mapToResponseIncludesMarkdownContentWhenRequested() {
		Activity activity = createTestActivity();

		ActivityMarkdown actMd = new ActivityMarkdown();
		actMd.setId(UUID.randomUUID());
		actMd.setActivity(activity);
		actMd.setType(MarkdownType.ARTIKULATIONSSCHEMA);
		actMd.setContent("# Test Schema");
		actMd.setCreatedAt(LocalDateTime.now());
		activity.getMarkdowns().add(actMd);

		ActivityResponse response = activityService.convertToResponse(activity, false, true);

		assertThat(response.getMarkdowns()).hasSize(1);
		MarkdownResponse mdResp = response.getMarkdowns().get(0);
		assertThat(mdResp.getId()).isEqualTo(actMd.getId());
		assertThat(mdResp.getType()).isEqualTo("artikulationsschema");
		assertThat(mdResp.getContent()).isEqualTo("# Test Schema");
	}

	@Test
	void mapToResponseKeepsOnlyLatestMarkdownPerType() {
		Activity activity = createTestActivity();

		ActivityMarkdown olderMarkdown = new ActivityMarkdown();
		olderMarkdown.setId(UUID.randomUUID());
		olderMarkdown.setActivity(activity);
		olderMarkdown.setType(MarkdownType.ARTIKULATIONSSCHEMA);
		olderMarkdown.setContent("# Old Schema");
		olderMarkdown.setCreatedAt(LocalDateTime.of(2026, 3, 1, 10, 0));

		ActivityMarkdown newerMarkdown = new ActivityMarkdown();
		newerMarkdown.setId(UUID.randomUUID());
		newerMarkdown.setActivity(activity);
		newerMarkdown.setType(MarkdownType.ARTIKULATIONSSCHEMA);
		newerMarkdown.setContent("# New Schema");
		newerMarkdown.setCreatedAt(LocalDateTime.of(2026, 3, 2, 10, 0));

		activity.getMarkdowns().add(olderMarkdown);
		activity.getMarkdowns().add(newerMarkdown);

		ActivityResponse response = activityService.convertToResponse(activity);

		assertThat(response.getMarkdowns()).hasSize(1);
		assertThat(response.getMarkdowns().get(0).getId()).isEqualTo(newerMarkdown.getId());
		assertThat(response.getMarkdowns().get(0).getContent()).isNull();
	}

	@Test
	void mapToResponseReturnsEmptyListsWhenNoDocumentsOrMarkdowns() {
		Activity activity = createTestActivity();

		ActivityResponse response = activityService.convertToResponse(activity);

		assertThat(response.getDocuments()).isEmpty();
		assertThat(response.getMarkdowns()).isEmpty();
	}

	@Test
	void getActivitiesWithFiltersMarksAuthenticatedUserFavourites() {
		UUID userId = UUID.randomUUID();
		Activity favouriteActivity = createTestActivity();
		Activity otherActivity = createTestActivity();
		UserFavourites favourite = new UserFavourites();
		favourite.setActivityId(favouriteActivity.getId());

		when(activityRepository.findAll(any(Specification.class)))
				.thenReturn(List.of(favouriteActivity, otherActivity));
		when(userFavouritesRepository.findByUserIdAndFavouriteTypeAndActivityIdIn(eq(userId), eq("activity"), any()))
				.thenReturn(List.of(favourite));

		List<ActivityResponse> responses = activityService.getActivitiesWithFilters(null, null, null, null, null, null,
				null, null, null, null, null, null, null, false, userId);

		assertThat(responses).hasSize(2);
		assertThat(responses.get(0).isFavourited()).isTrue();
		assertThat(responses.get(1).isFavourited()).isFalse();
	}

	@Test
	void getActivitiesWithFiltersDoesNotLoadFavouritesForAnonymousUsers() {
		Activity activity = createTestActivity();
		when(activityRepository.findAll(any(Specification.class))).thenReturn(List.of(activity));

		List<ActivityResponse> responses = activityService.getActivitiesWithFilters(null, null, null, null, null, null,
				null, null, null, null, null, null, null, false, null);

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).isFavourited()).isFalse();
		verify(userFavouritesRepository, never()).findByUserIdAndFavouriteTypeAndActivityIdIn(any(), any(), any());
	}

	@Test
	void updateActivityUpdatesExistingMarkdown() {
		Activity existingActivity = createTestActivity();
		existingActivity.setId(UUID.randomUUID());

		ActivityMarkdown existingMd = new ActivityMarkdown();
		existingMd.setId(UUID.randomUUID());
		existingMd.setActivity(existingActivity);
		existingMd.setType(MarkdownType.ARTIKULATIONSSCHEMA);
		existingMd.setContent("Old content");
		existingMd.setCreatedAt(LocalDateTime.now());
		existingActivity.getMarkdowns().add(existingMd);

		when(activityRepository.findById(existingActivity.getId())).thenReturn(Optional.of(existingActivity));
		when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

		Map<String, Object> updateData = new HashMap<>();
		updateData.put("name", "Updated Activity");
		updateData.put("description", existingActivity.getDescription());
		updateData.put("ageMin", existingActivity.getAgeMin());
		updateData.put("ageMax", existingActivity.getAgeMax());
		updateData.put("format", "unplugged");
		updateData.put("bloomLevel", "apply");
		updateData.put("durationMinMinutes", 30);
		updateData.put("artikulationsschemaMarkdown", "New content");

		ActivityResponse response = activityService.updateActivityFromMap(existingActivity.getId(), updateData);

		// Existing markdown should be updated, not a new one created
		assertThat(existingActivity.getMarkdowns()).hasSize(1);
		assertThat(existingActivity.getMarkdowns().get(0).getContent()).isEqualTo("New content");
		assertThat(response.getMarkdowns()).hasSize(1);
		assertThat(response.getMarkdowns().get(0).getContent()).isNull();
	}

	@Test
	void updateActivityLeavesMarkdownContentAloneWhenNotProvided() {
		Activity existingActivity = createTestActivity();
		existingActivity.setId(UUID.randomUUID());

		ActivityMarkdown existingMd = new ActivityMarkdown();
		existingMd.setId(UUID.randomUUID());
		existingMd.setActivity(existingActivity);
		existingMd.setType(MarkdownType.DECKBLATT);
		existingMd.setContent("Legacy\u2014content");
		existingMd.setCreatedAt(LocalDateTime.now());
		existingActivity.getMarkdowns().add(existingMd);

		when(activityRepository.findById(existingActivity.getId())).thenReturn(Optional.of(existingActivity));
		when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

		Map<String, Object> updateData = new HashMap<>();
		updateData.put("name", "Updated Activity");
		updateData.put("description", existingActivity.getDescription());
		updateData.put("ageMin", existingActivity.getAgeMin());
		updateData.put("ageMax", existingActivity.getAgeMax());
		updateData.put("format", "unplugged");
		updateData.put("bloomLevel", "apply");
		updateData.put("durationMinMinutes", 30);

		activityService.updateActivityFromMap(existingActivity.getId(), updateData);

		assertThat(existingActivity.getMarkdowns()).hasSize(1);
		assertThat(existingActivity.getMarkdowns().get(0).getContent()).isEqualTo("Legacy\u2014content");
	}

	@Test
	void updateActivityCreatesMarkdownWhenNonExistent() {
		Activity existingActivity = createTestActivity();
		existingActivity.setId(UUID.randomUUID());
		// No existing markdown

		when(activityRepository.findById(existingActivity.getId())).thenReturn(Optional.of(existingActivity));
		when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

		Map<String, Object> updateData = new HashMap<>();
		updateData.put("name", "Updated Activity");
		updateData.put("description", existingActivity.getDescription());
		updateData.put("ageMin", existingActivity.getAgeMin());
		updateData.put("ageMax", existingActivity.getAgeMax());
		updateData.put("format", "unplugged");
		updateData.put("bloomLevel", "apply");
		updateData.put("durationMinMinutes", 30);
		updateData.put("artikulationsschemaMarkdown", "New schema");

		ActivityResponse response = activityService.updateActivityFromMap(existingActivity.getId(), updateData);

		assertThat(existingActivity.getMarkdowns()).hasSize(1);
		assertThat(existingActivity.getMarkdowns().get(0).getContent()).isEqualTo("New schema");
		assertThat(existingActivity.getMarkdowns().get(0).getType()).isEqualTo(MarkdownType.ARTIKULATIONSSCHEMA);
		assertThat(response.getMarkdowns()).hasSize(1);
		assertThat(response.getMarkdowns().get(0).getContent()).isNull();
	}

	@Test
	void uploadPdfAndExtractMetadataSkipsLlmWhenDisabled() throws Exception {
		MockMultipartFile pdfFile = new MockMultipartFile("pdf_file", "activity.pdf", "application/pdf",
				"fake-pdf".getBytes(StandardCharsets.UTF_8));
		UUID cacheKey = UUID.randomUUID();

		when(pdfService.cachePdf(any(byte[].class), any(String.class))).thenReturn(cacheKey);

		Map<String, Object> result = activityService.uploadPdfAndExtractMetadata(pdfFile, false);

		assertThat(result.get("documentId")).isEqualTo(cacheKey.toString());
		assertThat(result.get("extractionConfidence")).isEqualTo(0.0);
		assertThat(result.get("extractionQuality")).isEqualTo("not_run");
		assertThat(result.get("extractedData")).isInstanceOf(Map.class);
		verify(llmService, never()).extractActivityData(any(String.class));
		verify(pdfService).updatePdfExtractionResults(cacheKey, Map.of(), null, "not_run");
	}

	@Test
	void extractMetadataFromDocumentUpdatesStoredResults() {
		UUID documentId = UUID.randomUUID();
		Map<String, Object> extractedData = new HashMap<>();
		extractedData.put("name", "Binary Bracelets");
		extractedData.put("description", "A complete activity description for students.");

		Map<String, Object> llmResponse = new HashMap<>();
		llmResponse.put("data", extractedData);
		llmResponse.put("confidence", 0.82d);

		when(pdfService.extractTextFromPdf(documentId)).thenReturn("PDF text");
		when(llmService.extractActivityData("PDF text")).thenReturn(llmResponse);

		Map<String, Object> result = activityService.extractMetadataFromDocument(documentId);

		assertThat(result.get("documentId")).isEqualTo(documentId.toString());
		assertThat(result.get("extractionQuality")).isEqualTo("high");
		assertThat(result.get("extractionConfidence")).isEqualTo(0.82d);
		verify(pdfService).updatePdfExtractionResults(documentId, extractedData, "0.820", "high");
	}

	@Test
	void extractMetadataFromDocumentNormalizesFlatLlmResponse() {
		UUID documentId = UUID.randomUUID();
		Map<String, Object> llmResponse = new HashMap<>();
		llmResponse.put("name", "Roboterspiel: Algorithmus im Labyrinth");
		llmResponse.put("description", "Schüler übernehmen in Gruppen die Rollen Programmierer, Computer und Roboter.");
		llmResponse.put("duration", "60 Minuten");
		llmResponse.put("materials", List.of("Kreppband", "Stifte", "Bauklötze"));
		llmResponse.put("bloom_taxonomy_level", "Analyze");
		llmResponse.put("topics", List.of("Algorithmen", "Roboter", "Fehlerkorrektur"));
		llmResponse.put("confidence", 0.91d);

		when(pdfService.extractTextFromPdf(documentId)).thenReturn("PDF text");
		when(llmService.extractActivityData("PDF text")).thenReturn(llmResponse);

		Map<String, Object> result = activityService.extractMetadataFromDocument(documentId);

		assertThat(result.get("extractionConfidence")).isEqualTo(0.91d);
		assertThat(result.get("extractionQuality")).isEqualTo("high");
		assertThat(result.get("extractedData")).isInstanceOf(Map.class);

		@SuppressWarnings("unchecked")
		Map<String, Object> extractedData = (Map<String, Object>) result.get("extractedData");
		assertThat(extractedData.get("name")).isEqualTo("Roboterspiel: Algorithmus im Labyrinth");
		assertThat(extractedData.get("durationMinMinutes")).isEqualTo(60);
		assertThat(extractedData.get("durationMaxMinutes")).isEqualTo(60);
		assertThat(extractedData.get("bloomLevel")).isEqualTo("analyze");
		assertThat(extractedData.get("resourcesNeeded")).isEqualTo(List.of("Kreppband", "Stifte", "Bauklötze"));

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(pdfService).updatePdfExtractionResults(any(UUID.class), captor.capture(), any(String.class),
				any(String.class));
		assertThat(captor.getValue().get("durationMinMinutes")).isEqualTo(60);
		assertThat(captor.getValue().get("bloomLevel")).isEqualTo("analyze");
	}

	@Test
	void createActivityWithValidationCreatesDocumentRelationship() throws Exception {
		UUID cacheKey = UUID.randomUUID();
		UUID finalizedDocId = UUID.randomUUID();
		PDFDocument finalizedDoc = createTestDocument(finalizedDocId);

		when(pdfService.finalizePdf(cacheKey)).thenReturn(finalizedDocId);
		when(pdfDocumentRepository.findById(finalizedDocId)).thenReturn(Optional.of(finalizedDoc));
		when(pdfDocumentRepository.save(any(PDFDocument.class))).thenAnswer(inv -> inv.getArgument(0));
		when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> {
			Activity a = inv.getArgument(0);
			a.setId(UUID.randomUUID());
			return a;
		});

		Map<String, Object> request = new HashMap<>();
		request.put("name", "Test Activity");
		request.put("description", "A test activity description");
		request.put("ageMin", 8);
		request.put("ageMax", 12);
		request.put("format", "unplugged");
		request.put("bloomLevel", "apply");
		request.put("durationMinMinutes", 30);
		request.put("documentId", cacheKey.toString());

		ActivityResponse response = activityService.createActivityWithValidation(request);

		assertThat(response).isNotNull();
		assertThat(response.getDocuments()).isEmpty();

		// Verify the saved activity has the document relationship
		ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
		verify(activityRepository).save(captor.capture());
		Activity saved = captor.getValue();
		assertThat(saved.getDocuments()).hasSize(1);
		assertThat(saved.getDocuments().get(0).getId()).isEqualTo(finalizedDocId);
		assertThat(saved.getDocuments().get(0).getType()).isEqualTo(DocumentType.SOURCE_PDF);
	}

	@Test
	void createActivitySanitizesMetadataAndMarkdownBeforeSaving() {
		Activity activity = createTestActivity();
		activity.setName("AI\u2014Titel");
		activity.setDescription("Zitat \u201Calt\u201D");
		activity.setSource("Quelle\u00A0A");
		activity.setResourcesNeeded(List.of("Papier\u200B", "Stift\u2013Set"));
		activity.setTopics(List.of("Logik\u2026"));

		ActivityMarkdown markdown = new ActivityMarkdown();
		markdown.setActivity(activity);
		markdown.setType(MarkdownType.DECKBLATT);
		markdown.setContent("Text\u2014mit\u201CZeichen\u201D");
		markdown.setCreatedAt(LocalDateTime.now());
		activity.getMarkdowns().add(markdown);

		when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> inv.getArgument(0));

		activityService.createActivity(activity);

		ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
		verify(activityRepository).save(captor.capture());
		Activity saved = captor.getValue();
		assertThat(saved.getName()).isEqualTo("AI-Titel");
		assertThat(saved.getDescription()).isEqualTo("Zitat \"alt\"");
		assertThat(saved.getSource()).isEqualTo("Quelle A");
		assertThat(saved.getResourcesNeeded()).containsExactly("Papier", "Stift-Set");
		assertThat(saved.getTopics()).containsExactly("Logik...");
		assertThat(saved.getMarkdowns().get(0).getContent()).isEqualTo("Text-mit\"Zeichen\"");
	}

	private Activity createTestActivity() {
		Activity activity = new Activity();
		activity.setId(UUID.randomUUID());
		activity.setName("Test Activity");
		activity.setDescription("A test activity");
		activity.setAgeMin(8);
		activity.setAgeMax(12);
		activity.setFormat(ActivityFormat.UNPLUGGED);
		activity.setBloomLevel(BloomLevel.APPLY);
		activity.setDurationMinMinutes(30);
		activity.setCreatedAt(LocalDateTime.now());
		return activity;
	}

	private PDFDocument createTestDocument(UUID docId) {
		PDFDocument doc = new PDFDocument();
		doc.setId(docId);
		doc.setFilename("test.pdf");
		doc.setFilePath("/tmp/test.pdf");
		doc.setFileSize(1024L);
		doc.setType(DocumentType.SOURCE_PDF);
		doc.setCreatedAt(LocalDateTime.now());
		return doc;
	}
}
