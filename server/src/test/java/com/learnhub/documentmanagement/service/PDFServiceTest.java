package com.learnhub.documentmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnhub.activitymanagement.dto.response.LessonPlanInfoResponse;
import com.learnhub.activitymanagement.entity.Activity;
import com.learnhub.activitymanagement.entity.ActivityMarkdown;
import com.learnhub.activitymanagement.entity.enums.DocumentType;
import com.learnhub.activitymanagement.entity.enums.MarkdownType;
import com.learnhub.activitymanagement.repository.ActivityRepository;
import com.learnhub.documentmanagement.entity.PDFDocument;
import com.learnhub.documentmanagement.repository.PDFDocumentRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class PDFServiceTest {

	private PDFService pdfService;
	private PDFDocumentRepository pdfDocumentRepository;
	private ActivityRepository activityRepository;
	private LLMService llmService;
	private MarkdownToHtmlService markdownToHtmlService;
	private MarkdownToPdfService markdownToPdfService;

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() {
		pdfService = new PDFService();
		pdfDocumentRepository = mock(PDFDocumentRepository.class);
		activityRepository = mock(ActivityRepository.class);
		llmService = mock(LLMService.class);
		markdownToHtmlService = mock(MarkdownToHtmlService.class);
		markdownToPdfService = mock(MarkdownToPdfService.class);

		ReflectionTestUtils.setField(pdfService, "pdfDocumentRepository", pdfDocumentRepository);
		ReflectionTestUtils.setField(pdfService, "activityRepository", activityRepository);
		ReflectionTestUtils.setField(pdfService, "llmService", llmService);
		ReflectionTestUtils.setField(pdfService, "markdownToHtmlService", markdownToHtmlService);
		ReflectionTestUtils.setField(pdfService, "markdownToPdfService", markdownToPdfService);
		ReflectionTestUtils.setField(pdfService, "pdfStoragePath", tempDir.toString());
	}

	@Test
	void cachePdfStoresInMemoryAndDoesNotPersist() {
		byte[] content = "fake-pdf-content".getBytes(StandardCharsets.UTF_8);

		UUID key = pdfService.cachePdf(content, "test.pdf");

		assertThat(key).isNotNull();
		// No interaction with DB
		verify(pdfDocumentRepository, never()).save(any());
		// Cache should contain the entry
		assertThat(pdfService.getPdfCache()).containsKey(key);
	}

	@Test
	void getPdfContentReturnsCachedContent() throws IOException {
		byte[] content = "cached-pdf".getBytes(StandardCharsets.UTF_8);
		UUID key = pdfService.cachePdf(content, "test.pdf");

		byte[] retrieved = pdfService.getPdfContent(key);

		assertThat(retrieved).isEqualTo(content);
		// Should not hit DB
		verify(pdfDocumentRepository, never()).findById(any());
	}

	@Test
	void getPdfContentFallsBackToDatabase() throws IOException {
		UUID docId = UUID.randomUUID();
		Path filePath = tempDir.resolve("existing.pdf");
		byte[] content = "db-pdf".getBytes(StandardCharsets.UTF_8);
		Files.write(filePath, content);

		PDFDocument document = new PDFDocument();
		document.setId(docId);
		document.setFilePath(filePath.toString());
		when(pdfDocumentRepository.findById(docId)).thenReturn(Optional.of(document));

		byte[] retrieved = pdfService.getPdfContent(docId);

		assertThat(retrieved).isEqualTo(content);
	}

	@Test
	void getPdfContentFallsBackToCurrentStorageDirectoryWhenStoredPathIsStale() throws IOException {
		UUID docId = UUID.randomUUID();
		String filename = "existing.pdf";
		Path localFilePath = tempDir.resolve(filename);
		byte[] content = "db-pdf".getBytes(StandardCharsets.UTF_8);
		Files.write(localFilePath, content);

		PDFDocument document = new PDFDocument();
		document.setId(docId);
		document.setFilePath("/app/data/pdfs/" + filename);
		when(pdfDocumentRepository.findById(docId)).thenReturn(Optional.of(document));

		byte[] retrieved = pdfService.getPdfContent(docId);

		assertThat(retrieved).isEqualTo(content);
	}

	@Test
	void getPdfDocumentReturnsCachedMetadata() {
		byte[] content = "cached-pdf".getBytes(StandardCharsets.UTF_8);
		UUID key = pdfService.cachePdf(content, "original_name.pdf");

		PDFDocument doc = pdfService.getPdfDocument(key);

		assertThat(doc.getId()).isEqualTo(key);
		assertThat(doc.getFilename()).isEqualTo("original_name.pdf");
		assertThat(doc.getFileSize()).isEqualTo(content.length);
		verify(pdfDocumentRepository, never()).findById(any());
	}

	@Test
	void finalizePdfPersistsToFilesystemAndDatabase() throws IOException {
		byte[] content = "finalize-me".getBytes(StandardCharsets.UTF_8);
		UUID key = pdfService.cachePdf(content, "activity.pdf");

		UUID generatedId = UUID.randomUUID();
		when(pdfDocumentRepository.save(any(PDFDocument.class))).thenAnswer(inv -> {
			PDFDocument doc = inv.getArgument(0);
			doc.setId(generatedId); // simulate JPA auto-generation
			return doc;
		});

		UUID docId = pdfService.finalizePdf(key);

		// Returned ID is the JPA-generated one, not the cache key
		assertThat(docId).isEqualTo(generatedId);
		assertThat(docId).isNotEqualTo(key);

		// Verify DB save
		ArgumentCaptor<PDFDocument> captor = ArgumentCaptor.forClass(PDFDocument.class);
		verify(pdfDocumentRepository).save(captor.capture());
		PDFDocument saved = captor.getValue();
		assertThat(saved.getFilename()).isEqualTo("activity.pdf");
		assertThat(saved.getFileSize()).isEqualTo(content.length);

		// Verify file written to filesystem (UUID prefix differs from cache key)
		assertThat(Files.list(tempDir).count()).isEqualTo(1);
		Path writtenFile = Files.list(tempDir).findFirst().get();
		assertThat(writtenFile.getFileName().toString()).endsWith("_activity.pdf");
		assertThat(Files.readAllBytes(writtenFile)).isEqualTo(content);

		// Cache should be empty after finalization
		assertThat(pdfService.getPdfCache()).doesNotContainKey(key);
	}

	@Test
	void finalizePdfThrowsWhenCacheKeyNotFound() {
		UUID unknownKey = UUID.randomUUID();
		assertThatThrownBy(() -> pdfService.finalizePdf(unknownKey)).isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Cached PDF not found");
	}

	@Test
	void updatePdfExtractionResultsUpdatesCacheWhenNotFinalized() {
		byte[] content = "pdf-content".getBytes(StandardCharsets.UTF_8);
		UUID key = pdfService.cachePdf(content, "test.pdf");

		Map<String, Object> fields = new HashMap<>();
		fields.put("name", "Test Activity");
		pdfService.updatePdfExtractionResults(key, fields, "0.850", "high");

		// Should not hit DB
		verify(pdfDocumentRepository, never()).findById(any());
		verify(pdfDocumentRepository, never()).save(any());

		// Verify cache was updated
		PDFDocument doc = pdfService.getPdfDocument(key);
		assertThat(doc.getConfidenceScore()).isEqualTo("0.850");
		assertThat(doc.getExtractionQuality()).isEqualTo("high");
	}

	@Test
	void sameFilenameCanBeUploadedMultipleTimes() throws IOException {
		byte[] content1 = "first-pdf".getBytes(StandardCharsets.UTF_8);
		byte[] content2 = "second-pdf".getBytes(StandardCharsets.UTF_8);
		UUID key1 = pdfService.cachePdf(content1, "activity.pdf");
		UUID key2 = pdfService.cachePdf(content2, "activity.pdf");

		assertThat(key1).isNotEqualTo(key2);
		assertThat(pdfService.getPdfContent(key1)).isEqualTo(content1);
		assertThat(pdfService.getPdfContent(key2)).isEqualTo(content2);

		// Finalize both — JPA generates distinct document IDs
		UUID genId1 = UUID.randomUUID();
		UUID genId2 = UUID.randomUUID();
		when(pdfDocumentRepository.save(any(PDFDocument.class))).thenAnswer(inv -> {
			PDFDocument d = inv.getArgument(0);
			d.setId(genId1);
			return d;
		}).thenAnswer(inv -> {
			PDFDocument d = inv.getArgument(0);
			d.setId(genId2);
			return d;
		});
		UUID docId1 = pdfService.finalizePdf(key1);
		UUID docId2 = pdfService.finalizePdf(key2);

		assertThat(docId1).isNotEqualTo(docId2);

		// Both files should exist with UUID-prefixed names (no collision)
		long fileCount = Files.list(tempDir).count();
		assertThat(fileCount).isEqualTo(2);
		// All files end with _activity.pdf
		Files.list(tempDir).forEach(path -> assertThat(path.getFileName().toString()).endsWith("_activity.pdf"));
	}

	@Test
	void evictExpiredCacheEntriesRemovesOldEntries() {
		byte[] content = "old-pdf".getBytes(StandardCharsets.UTF_8);
		UUID key = pdfService.cachePdf(content, "old.pdf");

		// Set upload time to well past the 60-minute cache TTL
		PDFService.CachedPdf cached = pdfService.getPdfCache().get(key);
		ReflectionTestUtils.setField(cached, "uploadedAt", LocalDateTime.now().minusMinutes(120));

		pdfService.evictExpiredCacheEntries();

		assertThat(pdfService.getPdfCache()).doesNotContainKey(key);
	}

	@Test
	void evictExpiredCacheEntriesKeepsFreshEntries() {
		byte[] content = "fresh-pdf".getBytes(StandardCharsets.UTF_8);
		UUID key = pdfService.cachePdf(content, "fresh.pdf");

		pdfService.evictExpiredCacheEntries();

		assertThat(pdfService.getPdfCache()).containsKey(key);
	}

	@Test
	void cachePdfEvictsOldestWhenMaxSizeReached() {
		// Fill cache to max size
		UUID oldest = null;
		for (int i = 0; i < 100; i++) {
			UUID k = pdfService.cachePdf(("pdf-" + i).getBytes(StandardCharsets.UTF_8), "file" + i + ".pdf");
			if (i == 0) {
				// Set the first entry to be older
				PDFService.CachedPdf cached = pdfService.getPdfCache().get(k);
				ReflectionTestUtils.setField(cached, "uploadedAt", LocalDateTime.now().minusMinutes(30));
				oldest = k;
			}
		}

		assertThat(pdfService.getPdfCache()).hasSize(100);

		// Adding one more should evict the oldest
		pdfService.cachePdf("overflow".getBytes(StandardCharsets.UTF_8), "overflow.pdf");

		assertThat(pdfService.getPdfCache()).hasSize(100);
		assertThat(pdfService.getPdfCache()).doesNotContainKey(oldest);
	}

	@Test
	void finalizePdfPreservesExtractionResultsFromCache() throws IOException {
		byte[] content = "pdf".getBytes(StandardCharsets.UTF_8);
		UUID key = pdfService.cachePdf(content, "test.pdf");

		Map<String, Object> fields = new HashMap<>();
		fields.put("name", "My Activity");
		pdfService.updatePdfExtractionResults(key, fields, "0.900", "high");

		UUID generatedId = UUID.randomUUID();
		when(pdfDocumentRepository.save(any(PDFDocument.class))).thenAnswer(inv -> {
			PDFDocument doc = inv.getArgument(0);
			doc.setId(generatedId);
			return doc;
		});
		UUID docId = pdfService.finalizePdf(key);

		assertThat(docId).isEqualTo(generatedId);

		ArgumentCaptor<PDFDocument> captor = ArgumentCaptor.forClass(PDFDocument.class);
		verify(pdfDocumentRepository).save(captor.capture());
		PDFDocument saved = captor.getValue();
		assertThat(saved.getConfidenceScore()).isEqualTo("0.900");
		assertThat(saved.getExtractionQuality()).isEqualTo("high");
		assertThat(saved.getExtractedFields()).contains("My Activity");
	}

	@Test
	void getLessonPlanInfoAcceptsDocumentIdsFromDocumentsArray() throws IOException {
		UUID documentId = UUID.randomUUID();
		Path filePath = tempDir.resolve("lesson-plan.pdf");
		Files.write(filePath, "pdf".getBytes(StandardCharsets.UTF_8));

		PDFDocument document = new PDFDocument();
		document.setId(documentId);
		document.setFilePath(filePath.toString());
		when(pdfDocumentRepository.findById(documentId)).thenReturn(Optional.of(document));

		LessonPlanInfoResponse response = pdfService
				.getLessonPlanInfo(List.of(Map.of("id", UUID.randomUUID().toString(), "documents",
						List.of(Map.of("id", documentId.toString(), "type", DocumentType.SOURCE_PDF.getValue())))));

		assertThat(response.isCanGenerateLessonPlan()).isTrue();
		assertThat(response.getAvailablePdfs()).isEqualTo(1);
		assertThat(response.getMissingPdfs()).isEmpty();
	}

	@Test
	void getLessonPlanInfoReturnsTrueWhenActivityHasMarkdowns() {
		UUID activityId = UUID.randomUUID();

		ActivityMarkdown md = new ActivityMarkdown();
		md.setType(MarkdownType.DECKBLATT);
		md.setContent("# Deckblatt\nSome content");
		md.setLandscape(false);

		Activity activity = new Activity();
		activity.setId(activityId);
		activity.setName("Test Activity");
		activity.getMarkdowns().add(md);

		when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));

		LessonPlanInfoResponse response = pdfService
				.getLessonPlanInfo(List.of(Map.of("id", activityId.toString())));

		assertThat(response.isCanGenerateLessonPlan()).isTrue();
		assertThat(response.getAvailablePdfs()).isEqualTo(1);
		assertThat(response.getMissingPdfs()).isEmpty();
	}

	@Test
	void getLessonPlanInfoFallsBackToSourcePdfWhenNoMarkdowns() throws IOException {
		UUID activityId = UUID.randomUUID();
		UUID documentId = UUID.randomUUID();
		Path filePath = tempDir.resolve("source.pdf");
		Files.write(filePath, "source-pdf".getBytes(StandardCharsets.UTF_8));

		PDFDocument sourcePdf = new PDFDocument();
		sourcePdf.setId(documentId);
		sourcePdf.setFilePath(filePath.toString());
		sourcePdf.setType(DocumentType.SOURCE_PDF);

		Activity activity = new Activity();
		activity.setId(activityId);
		activity.setName("Test Activity");
		activity.getDocuments().add(sourcePdf);
		// No markdowns

		when(activityRepository.findById(activityId)).thenReturn(Optional.of(activity));

		LessonPlanInfoResponse response = pdfService
				.getLessonPlanInfo(List.of(Map.of("id", activityId.toString())));

		assertThat(response.isCanGenerateLessonPlan()).isTrue();
		assertThat(response.getAvailablePdfs()).isEqualTo(1);
		assertThat(response.getMissingPdfs()).isEmpty();
	}

	@Test
	void getLessonPlanInfoReturnsTrueWhenRequestMapHasMarkdowns() {
		// Activity map contains markdowns in the request body (no DB lookup needed)
		Map<String, Object> activityMap = new HashMap<>();
		activityMap.put("id", UUID.randomUUID().toString());
		activityMap.put("name", "Test Activity");
		activityMap.put("markdowns", List.of(
				Map.of("type", "deckblatt", "content", "# Deckblatt\nContent", "landscape", false)));

		// No DB activity needed – markdowns are in the request map itself
		when(activityRepository.findById(any())).thenReturn(Optional.empty());

		LessonPlanInfoResponse response = pdfService.getLessonPlanInfo(List.of(activityMap));

		assertThat(response.isCanGenerateLessonPlan()).isTrue();
		assertThat(response.getAvailablePdfs()).isEqualTo(1);
		assertThat(response.getMissingPdfs()).isEmpty();
	}
}
