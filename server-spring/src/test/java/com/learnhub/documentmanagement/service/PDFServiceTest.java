package com.learnhub.documentmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnhub.activitymanagement.repository.ActivityRepository;
import com.learnhub.documentmanagement.entity.PDFDocument;
import com.learnhub.documentmanagement.repository.PDFDocumentRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
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

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() {
		pdfService = new PDFService();
		pdfDocumentRepository = mock(PDFDocumentRepository.class);
		activityRepository = mock(ActivityRepository.class);
		llmService = mock(LLMService.class);

		ReflectionTestUtils.setField(pdfService, "pdfDocumentRepository", pdfDocumentRepository);
		ReflectionTestUtils.setField(pdfService, "activityRepository", activityRepository);
		ReflectionTestUtils.setField(pdfService, "llmService", llmService);
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

		when(pdfDocumentRepository.save(any(PDFDocument.class))).thenAnswer(inv -> inv.getArgument(0));

		UUID docId = pdfService.finalizePdf(key);

		assertThat(docId).isEqualTo(key);

		// Verify DB save
		ArgumentCaptor<PDFDocument> captor = ArgumentCaptor.forClass(PDFDocument.class);
		verify(pdfDocumentRepository).save(captor.capture());
		PDFDocument saved = captor.getValue();
		assertThat(saved.getId()).isEqualTo(key);
		assertThat(saved.getFilename()).isEqualTo("activity.pdf");
		assertThat(saved.getFileSize()).isEqualTo(content.length);

		// Verify file written to filesystem with UUID-prefixed name
		Path expectedFile = tempDir.resolve(key + "_activity.pdf");
		assertThat(Files.exists(expectedFile)).isTrue();
		assertThat(Files.readAllBytes(expectedFile)).isEqualTo(content);

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

		// Finalize both
		when(pdfDocumentRepository.save(any(PDFDocument.class))).thenAnswer(inv -> inv.getArgument(0));
		pdfService.finalizePdf(key1);
		pdfService.finalizePdf(key2);

		// Both files should exist with UUID-prefixed names (no collision)
		Path file1 = tempDir.resolve(key1 + "_activity.pdf");
		Path file2 = tempDir.resolve(key2 + "_activity.pdf");
		assertThat(Files.exists(file1)).isTrue();
		assertThat(Files.exists(file2)).isTrue();
		assertThat(Files.readAllBytes(file1)).isEqualTo(content1);
		assertThat(Files.readAllBytes(file2)).isEqualTo(content2);
	}

	@Test
	void evictExpiredCacheEntriesRemovesOldEntries() {
		byte[] content = "old-pdf".getBytes(StandardCharsets.UTF_8);
		UUID key = pdfService.cachePdf(content, "old.pdf");

		// Manually set upload time to past
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

		when(pdfDocumentRepository.save(any(PDFDocument.class))).thenAnswer(inv -> inv.getArgument(0));
		pdfService.finalizePdf(key);

		ArgumentCaptor<PDFDocument> captor = ArgumentCaptor.forClass(PDFDocument.class);
		verify(pdfDocumentRepository).save(captor.capture());
		PDFDocument saved = captor.getValue();
		assertThat(saved.getConfidenceScore()).isEqualTo("0.900");
		assertThat(saved.getExtractionQuality()).isEqualTo("high");
		assertThat(saved.getExtractedFields()).contains("My Activity");
	}
}
