package com.learnhub.documentmanagement.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DocxCacheService {

	private static final Logger logger = LoggerFactory.getLogger(DocxCacheService.class);

	private final AdobePdfToDocxService adobePdfToDocxService;

	@Value("${learnhub.docx.cache.path:/app/data/docx-cache}")
	private String cachePath;

	public DocxCacheService(AdobePdfToDocxService adobePdfToDocxService) {
		this.adobePdfToDocxService = adobePdfToDocxService;
	}

	/**
	 * Returns a cached DOCX for {@code markdownId} if its file is newer than
	 * {@code contentTimestamp}; otherwise calls {@code generator}, writes the
	 * result, and returns it.
	 */
	public byte[] getOrGenerate(UUID markdownId, LocalDateTime contentTimestamp, Supplier<byte[]> generator)
			throws IOException {
		if (!adobePdfToDocxService.isConfigured()) {
			return generator.get();
		}
		initCacheDir();
		Path docxPath = resolve(markdownId + ".docx");
		if (isCacheValid(docxPath, contentTimestamp)) {
			logger.debug("DOCX cache hit for markdown {}", markdownId);
			return Files.readAllBytes(docxPath);
		}
		logger.debug("DOCX cache miss for markdown {}, generating", markdownId);
		byte[] docx = generator.get();
		Files.write(docxPath, docx);
		return docx;
	}

	/**
	 * Returns a cached DOCX for the merged activity if the file is newer than the
	 * latest of {@code markdownTimestamps}; otherwise regenerates.
	 */
	public byte[] getOrGenerateMerged(UUID activityId, List<LocalDateTime> markdownTimestamps,
			Supplier<byte[]> generator) throws IOException {
		if (!adobePdfToDocxService.isConfigured()) {
			return generator.get();
		}
		initCacheDir();
		Path docxPath = resolve("activity_" + activityId + ".docx");
		LocalDateTime maxTimestamp = markdownTimestamps.stream().filter(Objects::nonNull).max(Comparator.naturalOrder())
				.orElse(null);
		if (isCacheValid(docxPath, maxTimestamp)) {
			logger.debug("DOCX cache hit for activity {}", activityId);
			return Files.readAllBytes(docxPath);
		}
		logger.debug("DOCX cache miss for activity {}, generating", activityId);
		byte[] docx = generator.get();
		Files.write(docxPath, docx);
		return docx;
	}

	private boolean isCacheValid(Path docxPath, LocalDateTime contentTimestamp) throws IOException {
		if (!Files.exists(docxPath) || contentTimestamp == null) {
			return false;
		}
		FileTime fileTime = Files.getLastModifiedTime(docxPath);
		Instant cachedAt = fileTime.toInstant();
		return cachedAt.isAfter(contentTimestamp.toInstant(ZoneOffset.UTC));
	}

	private void initCacheDir() throws IOException {
		Files.createDirectories(Paths.get(cachePath));
	}

	public void evictForActivity(UUID activityId, List<UUID> markdownIds) {
		deleteIfExists(resolve("activity_" + activityId + ".docx"));
		for (UUID markdownId : markdownIds) {
			deleteIfExists(resolve(markdownId + ".docx"));
		}
	}

	private void deleteIfExists(Path path) {
		try {
			if (Files.deleteIfExists(path)) {
				logger.debug("Deleted DOCX cache file: {}", path);
			}
		} catch (IOException e) {
			logger.warn("Failed to delete DOCX cache file {}: {}", path, e.getMessage());
		}
	}

	private Path resolve(String filename) {
		return Paths.get(cachePath, filename);
	}
}
