package com.learnhub.activitymanagement.service;

import com.learnhub.documentmanagement.service.LLMService;
import com.learnhub.documentmanagement.service.PDFService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service responsible for metadata extraction via LLM and data normalization
 * for admin draft editing workflows.
 *
 * <p>
 * Extracted from {@link ActivityService} to keep that class focused on core
 * CRUD and filtering logic.
 * </p>
 */
@Service
public class ActivityExtractionService {

	private static final Logger logger = LoggerFactory.getLogger(ActivityExtractionService.class);
	private static final Pattern FIRST_INTEGER_PATTERN = Pattern.compile("(\\d+)");

	@Autowired
	private PDFService pdfService;

	@Autowired
	private LLMService llmService;

	/**
	 * Extract metadata from a cached or persisted PDF.
	 */
	public Map<String, Object> extractMetadataFromDocument(UUID documentIdOrCacheKey) {
		try {
			String pdfText = pdfService.extractTextFromPdf(documentIdOrCacheKey);
			Map<String, Object> extractionResult = llmService.extractActivityData(pdfText);
			Map<String, Object> extractedData = extractActivityDataMap(extractionResult);
			Double confidence = extractionResult.get("confidence") instanceof Number
					? ((Number) extractionResult.get("confidence")).doubleValue()
					: 0.0;

			String extractionQuality = determineExtractionQuality(confidence);
			String confidenceScore = String.format("%.3f", confidence);
			pdfService.updatePdfExtractionResults(documentIdOrCacheKey, extractedData, confidenceScore,
					extractionQuality);

			return buildExtractionResponse(documentIdOrCacheKey, extractedData, confidence, extractionQuality);
		} catch (Exception e) {
			throw new RuntimeException("Failed to extract metadata: " + e.getMessage(), e);
		}
	}

	// ---- Data extraction and normalization ----

	private Map<String, Object> extractActivityDataMap(Map<String, Object> extractionResult) {
		if (extractionResult == null || extractionResult.isEmpty()) {
			throw new RuntimeException("LLM extraction did not return a valid data map");
		}

		Object dataObj = extractionResult.get("data");
		if (dataObj instanceof Map<?, ?> rawMap) {
			@SuppressWarnings("unchecked")
			Map<String, Object> extractedData = new HashMap<>((Map<String, Object>) rawMap);
			return normalizeExtractedData(extractedData);
		}

		if (looksLikeActivityData(extractionResult)) {
			return normalizeExtractedData(new HashMap<>(extractionResult));
		}

		throw new RuntimeException("LLM extraction did not return a valid data map");
	}

	private boolean looksLikeActivityData(Map<String, Object> candidate) {
		return candidate.containsKey("name") || candidate.containsKey("description")
				|| candidate.containsKey("duration") || candidate.containsKey("materials")
				|| candidate.containsKey("bloom_taxonomy_level");
	}

	Map<String, Object> normalizeExtractedData(Map<String, Object> data) {
		copyAlias(data, "title", "name");
		copyAlias(data, "summary", "description");
		copyAlias(data, "materials", "resourcesNeeded");
		copyAlias(data, "bloom_taxonomy_level", "bloomLevel");
		copyAlias(data, "bloomTaxonomyLevel", "bloomLevel");

		Object duration = data.get("duration");
		if (duration != null) {
			Integer parsedDuration = extractFirstInteger(duration);
			if (parsedDuration != null) {
				data.putIfAbsent("durationMinMinutes", parsedDuration);
				data.putIfAbsent("durationMaxMinutes", parsedDuration);
			}
		}

		Object bloomLevel = data.get("bloomLevel");
		if (bloomLevel != null) {
			data.put("bloomLevel", bloomLevel.toString().trim().toLowerCase());
		}

		Object resourcesNeeded = data.get("resourcesNeeded");
		if (resourcesNeeded instanceof List<?> rawList) {
			List<String> normalized = new ArrayList<>();
			for (Object item : rawList) {
				if (item != null) {
					normalized.add(item.toString());
				}
			}
			data.put("resourcesNeeded", normalized);
		}

		Object topics = data.get("topics");
		if (topics instanceof List<?> rawTopics) {
			List<String> normalizedTopics = new ArrayList<>();
			for (Object item : rawTopics) {
				if (item != null) {
					normalizedTopics.add(item.toString());
				}
			}
			data.put("topics", normalizedTopics);
		}

		return data;
	}

	private void copyAlias(Map<String, Object> data, String sourceKey, String targetKey) {
		if (!data.containsKey(targetKey) && data.get(sourceKey) != null) {
			data.put(targetKey, data.get(sourceKey));
		}
	}

	Integer extractFirstInteger(Object value) {
		if (value instanceof Number number) {
			return number.intValue();
		}

		Matcher matcher = FIRST_INTEGER_PATTERN.matcher(value.toString());
		if (matcher.find()) {
			return Integer.parseInt(matcher.group(1));
		}

		return null;
	}

	/**
	 * Determine extraction quality based on confidence.
	 */
	String determineExtractionQuality(Double confidence) {
		if (confidence < 0.5) {
			return "low";
		} else if (confidence < 0.75) {
			return "medium";
		}
		return "high";
	}

	/**
	 * Apply default values to activity data.
	 */
	Map<String, Object> applyActivityDefaults(Map<String, Object> extractedData) {
		Map<String, Object> activityData = new HashMap<>(extractedData);

		if (!activityData.containsKey("ageMin"))
			activityData.put("ageMin", 6);
		if (!activityData.containsKey("ageMax"))
			activityData.put("ageMax", 12);
		if (!activityData.containsKey("format"))
			activityData.put("format", "unplugged");
		if (!activityData.containsKey("bloomLevel"))
			activityData.put("bloomLevel", "remember");
		if (!activityData.containsKey("durationMinMinutes"))
			activityData.put("durationMinMinutes", 15);
		if (!activityData.containsKey("mentalLoad"))
			activityData.put("mentalLoad", "medium");
		if (!activityData.containsKey("physicalEnergy"))
			activityData.put("physicalEnergy", "medium");
		if (!activityData.containsKey("prepTimeMinutes"))
			activityData.put("prepTimeMinutes", 5);
		if (!activityData.containsKey("cleanupTimeMinutes"))
			activityData.put("cleanupTimeMinutes", 5);

		return activityData;
	}

	private Map<String, Object> buildExtractionResponse(UUID documentId, Map<String, Object> extractedData,
			Double confidence, String extractionQuality) {
		Map<String, Object> response = new HashMap<>();
		response.put("documentId", documentId.toString());
		response.put("extractedData", applyActivityDefaults(extractedData));
		response.put("extractionConfidence", confidence);
		response.put("extractionQuality", extractionQuality);
		return response;
	}
}
