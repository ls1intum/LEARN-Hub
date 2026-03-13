package com.learnhub.activitymanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnhub.activitymanagement.dto.response.ActivityResponse;
import com.learnhub.activitymanagement.dto.response.DocumentResponse;
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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class ActivityServiceTest {

	private ActivityService activityService;
	private ActivityRepository activityRepository;
	private PDFDocumentRepository pdfDocumentRepository;
	private PDFService pdfService;
	private LLMService llmService;

	@BeforeEach
	void setUp() {
		activityService = new ActivityService();
		activityRepository = mock(ActivityRepository.class);
		pdfDocumentRepository = mock(PDFDocumentRepository.class);
		pdfService = mock(PDFService.class);
		llmService = mock(LLMService.class);

		ReflectionTestUtils.setField(activityService, "activityRepository", activityRepository);
		ReflectionTestUtils.setField(activityService, "pdfDocumentRepository", pdfDocumentRepository);
		ReflectionTestUtils.setField(activityService, "pdfService", pdfService);
		ReflectionTestUtils.setField(activityService, "llmService", llmService);
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
	void mapToResponseReturnsDocumentsList() {
		Activity activity = createTestActivity();
		UUID docId = UUID.randomUUID();

		PDFDocument doc = createTestDocument(docId);
		activity.getDocuments().add(doc);

		ActivityResponse response = activityService.convertToResponse(activity);

		assertThat(response.getDocuments()).hasSize(1);
		DocumentResponse docResp = response.getDocuments().get(0);
		assertThat(docResp.getId()).isEqualTo(docId);
		assertThat(docResp.getType()).isEqualTo("source_pdf");
		assertThat(docResp.getFilename()).isEqualTo("test.pdf");
		assertThat(docResp.getFileSize()).isEqualTo(1024L);
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
		assertThat(mdResp.getContent()).isEqualTo("# Test Schema");
	}

	@Test
	void mapToResponseReturnsEmptyListsWhenNoDocumentsOrMarkdowns() {
		Activity activity = createTestActivity();

		ActivityResponse response = activityService.convertToResponse(activity);

		assertThat(response.getDocuments()).isEmpty();
		assertThat(response.getMarkdowns()).isEmpty();
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
		assertThat(response.getMarkdowns().get(0).getContent()).isEqualTo("New content");
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
		assertThat(response.getMarkdowns().get(0).getContent()).isEqualTo("New schema");
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
		assertThat(response.getDocuments()).hasSize(1);
		assertThat(response.getDocuments().get(0).getId()).isEqualTo(finalizedDocId);
		assertThat(response.getDocuments().get(0).getType()).isEqualTo("source_pdf");

		// Verify the saved activity has the document relationship
		ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
		verify(activityRepository).save(captor.capture());
		Activity saved = captor.getValue();
		assertThat(saved.getDocuments()).hasSize(1);
		assertThat(saved.getDocuments().get(0).getId()).isEqualTo(finalizedDocId);
		assertThat(saved.getDocuments().get(0).getType()).isEqualTo(DocumentType.SOURCE_PDF);
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
