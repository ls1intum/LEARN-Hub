package com.learnhub.activitymanagement.service;

import com.learnhub.activitymanagement.dto.response.ActivityResponse;
import com.learnhub.activitymanagement.entity.Activity;
import com.learnhub.activitymanagement.entity.ActivityMarkdown;
import com.learnhub.activitymanagement.entity.enums.*;
import com.learnhub.activitymanagement.repository.ActivityRepository;
import com.learnhub.documentmanagement.entity.PDFDocument;
import com.learnhub.documentmanagement.repository.PDFDocumentRepository;
import com.learnhub.documentmanagement.service.LLMService;
import com.learnhub.documentmanagement.service.PDFService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ActivityService {

	private static final Logger logger = LoggerFactory.getLogger(ActivityService.class);

	@Autowired
	private ActivityRepository activityRepository;

	@Autowired
	private PDFDocumentRepository pdfDocumentRepository;

	@Autowired
	private PDFService pdfService;

	@Autowired
	private LLMService llmService;

	public List<ActivityResponse> getAllActivities() {
		return activityRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
	}

	public long countAllActivities() {
		return activityRepository.count();
	}

	public long countActivitiesWithFilters(String name, Integer ageMin, Integer ageMax, Integer durationMin,
			Integer durationMax, List<String> formats, List<String> bloomLevels, String mentalLoad,
			String physicalEnergy, List<String> resourcesNeeded, List<String> topics) {
		return getFilteredActivities(name, ageMin, ageMax, durationMin, durationMax, formats, bloomLevels, mentalLoad,
				physicalEnergy, resourcesNeeded, topics).size();
	}

	public List<ActivityResponse> getActivitiesWithFilters(String name, Integer ageMin, Integer ageMax,
			Integer durationMin, Integer durationMax, List<String> formats, List<String> bloomLevels, String mentalLoad,
			String physicalEnergy, List<String> resourcesNeeded, List<String> topics, Integer limit, Integer offset) {
		List<Activity> allMatching = getFilteredActivities(name, ageMin, ageMax, durationMin, durationMax, formats,
				bloomLevels, mentalLoad, physicalEnergy, resourcesNeeded, topics);

		int start = Math.min((offset != null && offset > 0) ? offset : 0, allMatching.size());
		int pageSize = (limit != null && limit > 0) ? limit : allMatching.size();
		int end = Math.min(start + pageSize, allMatching.size());

		return allMatching.subList(start, end).stream().map(this::mapToResponse).collect(Collectors.toList());
	}

	private List<Activity> getFilteredActivities(String name, Integer ageMin, Integer ageMax, Integer durationMin,
			Integer durationMax, List<String> formats, List<String> bloomLevels, String mentalLoad,
			String physicalEnergy, List<String> resourcesNeeded, List<String> topics) {

		Specification<Activity> spec = Specification.where(null);

		if (name != null && !name.isEmpty()) {
			spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
		}

		if (ageMin != null) {
			spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("ageMin"), ageMin));
		}

		if (ageMax != null) {
			spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("ageMax"), ageMax));
		}

		if (formats != null && !formats.isEmpty()) {
			List<ActivityFormat> formatEnums = formats.stream().map(ActivityFormat::fromValue)
					.collect(Collectors.toList());
			spec = spec.and((root, query, cb) -> root.get("format").in(formatEnums));
		}

		if (bloomLevels != null && !bloomLevels.isEmpty()) {
			List<BloomLevel> bloomEnums = bloomLevels.stream().map(BloomLevel::fromValue).collect(Collectors.toList());
			spec = spec.and((root, query, cb) -> root.get("bloomLevel").in(bloomEnums));
		}

		if (durationMin != null) {
			spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("durationMinMinutes"), durationMin));
		}

		if (durationMax != null) {
			spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("durationMaxMinutes"), durationMax));
		}

		if (mentalLoad != null && !mentalLoad.isEmpty()) {
			EnergyLevel energyLevel = convertStringToEnergyLevel(mentalLoad);
			spec = spec.and((root, query, cb) -> cb.equal(root.get("mentalLoad"), energyLevel));
		}

		if (physicalEnergy != null && !physicalEnergy.isEmpty()) {
			EnergyLevel energyLevel = convertStringToEnergyLevel(physicalEnergy);
			spec = spec.and((root, query, cb) -> cb.equal(root.get("physicalEnergy"), energyLevel));
		}

		List<Activity> allMatching = activityRepository.findAll(spec);

		// Apply in-memory filtering for JSONB-backed list fields
		if (resourcesNeeded != null && !resourcesNeeded.isEmpty()) {
			Set<String> neededLower = resourcesNeeded.stream().map(String::toLowerCase).collect(Collectors.toSet());
			allMatching = allMatching.stream()
					.filter(a -> a.getResourcesNeeded() != null && a.getResourcesNeeded().stream()
							.filter(r -> r != null).map(String::toLowerCase).anyMatch(neededLower::contains))
					.collect(Collectors.toList());
		}

		if (topics != null && !topics.isEmpty()) {
			Set<String> topicsLower = topics.stream().map(String::toLowerCase).collect(Collectors.toSet());
			allMatching = allMatching.stream().filter(a -> a.getTopics() != null && a.getTopics().stream()
					.filter(t -> t != null).map(String::toLowerCase).anyMatch(topicsLower::contains))
					.collect(Collectors.toList());
		}
		return allMatching;
	}

	private EnergyLevel convertStringToEnergyLevel(String value) {
		switch (value.toLowerCase()) {
			case "low" :
				return EnergyLevel.LOW;
			case "medium" :
				return EnergyLevel.MEDIUM;
			case "high" :
				return EnergyLevel.HIGH;
			default :
				throw new IllegalArgumentException(
						"Invalid energy level: " + value + ". Must be 'low', 'medium', or 'high'");
		}
	}

	public ActivityResponse getActivityById(UUID id) {
		logger.debug("Fetching activity by id={}", id);
		Activity activity = activityRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Activity not found"));
		return mapToResponse(activity);
	}

	public ActivityResponse createActivity(Activity activity) {
		logger.debug("Saving new activity: name={}", activity.getName());
		Activity saved = activityRepository.save(activity);
		logger.debug("Activity saved with id={}", saved.getId());
		return mapToResponse(saved);
	}

	public ActivityResponse updateActivity(UUID id, Activity activityUpdate) {
		Activity activity = activityRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Activity not found"));

		// Update fields
		activity.setName(activityUpdate.getName());
		activity.setDescription(activityUpdate.getDescription());
		activity.setSource(activityUpdate.getSource());
		activity.setAgeMin(activityUpdate.getAgeMin());
		activity.setAgeMax(activityUpdate.getAgeMax());
		activity.setFormat(activityUpdate.getFormat());
		activity.setBloomLevel(activityUpdate.getBloomLevel());
		activity.setDurationMinMinutes(activityUpdate.getDurationMinMinutes());
		activity.setDurationMaxMinutes(activityUpdate.getDurationMaxMinutes());
		activity.setMentalLoad(activityUpdate.getMentalLoad());
		activity.setPhysicalEnergy(activityUpdate.getPhysicalEnergy());
		activity.setPrepTimeMinutes(activityUpdate.getPrepTimeMinutes());
		activity.setCleanupTimeMinutes(activityUpdate.getCleanupTimeMinutes());
		activity.setResourcesNeeded(activityUpdate.getResourcesNeeded());
		activity.setTopics(activityUpdate.getTopics());

		// Update artikulationsschema markdown if provided
		Optional<ActivityMarkdown> newMarkdown = activityUpdate.getMarkdowns().stream()
				.filter(m -> m.getType() == MarkdownType.ARTIKULATIONSSCHEMA).findFirst();
		if (newMarkdown.isPresent()) {
			String newContent = newMarkdown.get().getContent();
			Optional<ActivityMarkdown> existing = activity.getMarkdowns().stream()
					.filter(m -> m.getType() == MarkdownType.ARTIKULATIONSSCHEMA).findFirst();
			if (existing.isPresent()) {
				existing.get().setContent(newContent);
			} else {
				ActivityMarkdown md = new ActivityMarkdown();
				md.setActivity(activity);
				md.setType(MarkdownType.ARTIKULATIONSSCHEMA);
				md.setContent(newContent);
				md.setCreatedAt(LocalDateTime.now());
				activity.getMarkdowns().add(md);
			}
		}

		Activity saved = activityRepository.save(activity);
		return mapToResponse(saved);
	}

	/**
	 * Update activity from a Map payload (used by the PUT endpoint). Re-uses
	 * createActivityFromMap to parse the request, then applies to the existing
	 * entity.
	 */
	public ActivityResponse updateActivityFromMap(UUID id, Map<String, Object> request) {
		Activity activityUpdate = createActivityFromMap(request);
		return updateActivity(id, activityUpdate);
	}

	public void deleteActivity(UUID id) {
		logger.debug("Deleting activity with id={}", id);
		activityRepository.deleteById(id);
	}

	public Activity createActivityFromMap(Map<String, Object> data) {
		Activity activity = new Activity();

		if (data.get("name") != null)
			activity.setName(data.get("name").toString());
		if (data.get("description") != null)
			activity.setDescription(data.get("description").toString());
		if (data.get("source") != null)
			activity.setSource(data.get("source").toString());

		if (data.get("ageMin") != null) {
			activity.setAgeMin(Integer.parseInt(data.get("ageMin").toString()));
		}
		if (data.get("ageMax") != null) {
			activity.setAgeMax(Integer.parseInt(data.get("ageMax").toString()));
		}

		if (data.get("format") != null) {
			activity.setFormat(ActivityFormat.fromValue(data.get("format").toString()));
		}
		if (data.get("bloomLevel") != null) {
			activity.setBloomLevel(BloomLevel.fromValue(data.get("bloomLevel").toString()));
		}

		if (data.get("durationMinMinutes") != null) {
			activity.setDurationMinMinutes(Integer.parseInt(data.get("durationMinMinutes").toString()));
		}
		if (data.get("durationMaxMinutes") != null) {
			activity.setDurationMaxMinutes(Integer.parseInt(data.get("durationMaxMinutes").toString()));
		}

		if (data.get("mentalLoad") != null) {
			activity.setMentalLoad(EnergyLevel.fromValue(data.get("mentalLoad").toString()));
		}
		if (data.get("physicalEnergy") != null) {
			activity.setPhysicalEnergy(EnergyLevel.fromValue(data.get("physicalEnergy").toString()));
		}

		if (data.get("prepTimeMinutes") != null) {
			activity.setPrepTimeMinutes(Integer.parseInt(data.get("prepTimeMinutes").toString()));
		}
		if (data.get("cleanupTimeMinutes") != null) {
			activity.setCleanupTimeMinutes(Integer.parseInt(data.get("cleanupTimeMinutes").toString()));
		}

		if (data.get("resourcesNeeded") != null) {
			activity.setResourcesNeeded((List<String>) data.get("resourcesNeeded"));
		}
		if (data.get("topics") != null) {
			activity.setTopics((List<String>) data.get("topics"));
		}

		if (data.get("documentId") != null) {
			try {
				UUID docId = UUID.fromString(data.get("documentId").toString());
				PDFDocument doc = pdfDocumentRepository.findById(docId).orElse(null);
				if (doc != null) {
					doc.setType(DocumentType.SOURCE_PDF);
					pdfDocumentRepository.save(doc);
					activity.getDocuments().add(doc);
				}
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Invalid documentId format: must be a valid UUID");
			}
		}

		if (data.get("artikulationsschemaMarkdown") != null) {
			ActivityMarkdown actMd = new ActivityMarkdown();
			actMd.setActivity(activity);
			actMd.setType(MarkdownType.ARTIKULATIONSSCHEMA);
			actMd.setContent(data.get("artikulationsschemaMarkdown").toString());
			actMd.setCreatedAt(LocalDateTime.now());
			activity.getMarkdowns().add(actMd);
		}

		return activity;
	}

	public ActivityResponse convertToResponse(Activity activity) {
		return mapToResponse(activity);
	}

	private ActivityResponse mapToResponse(Activity activity) {
		ActivityResponse response = new ActivityResponse();
		response.setId(activity.getId());
		response.setName(activity.getName());
		response.setDescription(activity.getDescription());
		response.setSource(activity.getSource());
		response.setAgeMin(activity.getAgeMin());
		response.setAgeMax(activity.getAgeMax());
		response.setFormat(activity.getFormat() != null ? activity.getFormat().getValue() : null);
		response.setBloomLevel(activity.getBloomLevel() != null ? activity.getBloomLevel().getValue() : null);
		response.setDurationMinMinutes(activity.getDurationMinMinutes());
		response.setDurationMaxMinutes(activity.getDurationMaxMinutes());
		response.setMentalLoad(activity.getMentalLoad() != null ? activity.getMentalLoad().getValue() : null);
		response.setPhysicalEnergy(
				activity.getPhysicalEnergy() != null ? activity.getPhysicalEnergy().getValue() : null);
		response.setPrepTimeMinutes(activity.getPrepTimeMinutes());
		response.setCleanupTimeMinutes(activity.getCleanupTimeMinutes());
		response.setResourcesNeeded(activity.getResourcesNeeded());
		response.setTopics(activity.getTopics());

		// Extract documentId from the first SOURCE_PDF document
		UUID documentId = activity.getDocuments().stream()
				.filter(d -> d.getType() == DocumentType.SOURCE_PDF)
				.findFirst()
				.map(PDFDocument::getId)
				.orElse(null);
		response.setDocumentId(documentId);

		// Extract artikulationsschema markdown from the first ARTIKULATIONSSCHEMA entry
		String markdown = activity.getMarkdowns().stream()
				.filter(m -> m.getType() == MarkdownType.ARTIKULATIONSSCHEMA)
				.findFirst()
				.map(ActivityMarkdown::getContent)
				.orElse(null);
		response.setArtikulationsschemaMarkdown(markdown);

		return response;
	}

	/**
	 * Create activity with validation
	 */
	public ActivityResponse createActivityWithValidation(Map<String, Object> request) {
		// Validate documentId (used as cache key or existing DB ID)
		Object documentIdObj = request.get("documentId");
		if (documentIdObj == null) {
			throw new IllegalArgumentException("documentId is required");
		}

		UUID cacheKeyOrDocId;
		try {
			cacheKeyOrDocId = UUID.fromString(documentIdObj.toString());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid documentId format: must be a valid UUID");
		}

		UUID documentId;
		// Finalize the cached PDF (persist to filesystem + DB), returns the real DB ID
		try {
			documentId = pdfService.finalizePdf(cacheKeyOrDocId);
		} catch (RuntimeException e) {
			// PDF was not in the cache – check if it already exists in the database
			try {
				byte[] pdfContent = pdfService.getPdfContent(cacheKeyOrDocId);
				if (pdfContent == null || pdfContent.length == 0) {
					throw new IllegalArgumentException("PDF document with ID " + cacheKeyOrDocId + " does not exist");
				}
				documentId = cacheKeyOrDocId;
			} catch (IllegalArgumentException ie) {
				throw ie;
			} catch (Exception ex) {
				throw new IllegalArgumentException("PDF document with ID " + cacheKeyOrDocId + " does not exist");
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to finalize PDF: " + e.getMessage(), e);
		}

		// Update request with the real documentId (may differ from cache key)
		request.put("documentId", documentId.toString());

		// Create activity from request
		Activity activity = createActivityFromMap(request);
		return createActivity(activity);
	}

	/**
	 * Upload PDF, cache it, and extract metadata using LLM. Returns document_id
	 * (cache key) and extracted data for the 2-step creation flow. The PDF is NOT
	 * persisted yet – call createActivityWithValidation to finalize.
	 */
	public Map<String, Object> uploadPdfAndExtractMetadata(MultipartFile pdfFile) {
		try {
			if (pdfFile.isEmpty()) {
				throw new IllegalArgumentException("No PDF file provided");
			}

			if (!pdfFile.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
				throw new IllegalArgumentException("File must be a PDF");
			}

			byte[] pdfContent = pdfFile.getBytes();
			if (pdfContent.length == 0) {
				throw new IllegalArgumentException("PDF file is empty");
			}

			UUID cacheKey = pdfService.cachePdf(pdfContent, pdfFile.getOriginalFilename());

			// Extract text and metadata using LLM
			String pdfText = pdfService.extractTextFromPdf(cacheKey);
			Map<String, Object> extractionResult = llmService.extractActivityData(pdfText);

			Object dataObj = extractionResult.get("data");
			if (!(dataObj instanceof Map)) {
				throw new RuntimeException("LLM extraction did not return a valid data map");
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> extractedData = (Map<String, Object>) dataObj;
			Double confidence = extractionResult.get("confidence") != null
					? (Double) extractionResult.get("confidence")
					: 0.0;

			String extractionQuality = determineExtractionQuality(confidence);

			// Update cached PDF with extraction results
			String confidenceScore = String.format("%.3f", confidence);
			pdfService.updatePdfExtractionResults(cacheKey, extractedData, confidenceScore, extractionQuality);

			// Apply defaults
			Map<String, Object> activityData = applyActivityDefaults(extractedData);

			Map<String, Object> response = new HashMap<>();
			response.put("documentId", cacheKey.toString());
			response.put("extractedData", activityData);
			response.put("extractionConfidence", confidence);
			response.put("extractionQuality", extractionQuality);

			return response;
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to upload PDF and extract metadata: " + e.getMessage(), e);
		}
	}

	/**
	 * Upload PDF and create activity with extracted data
	 */
	public Map<String, Object> uploadAndCreateActivity(MultipartFile pdfFile) {
		try {
			if (pdfFile.isEmpty()) {
				throw new IllegalArgumentException("No PDF file provided");
			}

			if (!pdfFile.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
				throw new IllegalArgumentException("File must be a PDF");
			}

			// Cache PDF in memory first
			byte[] pdfContent = pdfFile.getBytes();
			if (pdfContent.length == 0) {
				throw new IllegalArgumentException("PDF file is empty");
			}

			UUID cacheKey = pdfService.cachePdf(pdfContent, pdfFile.getOriginalFilename());

			// Extract activity data using LLM
			String pdfText = new String(pdfContent); // Simplified - should use PDF parser
			Map<String, Object> extractionResult = llmService.extractActivityData(pdfText);

			Object dataObj = extractionResult.get("data");
			if (!(dataObj instanceof Map)) {
				throw new RuntimeException("LLM extraction did not return a valid data map");
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> extractedData = (Map<String, Object>) dataObj;
			Double confidence = extractionResult.get("confidence") != null
					? (Double) extractionResult.get("confidence")
					: 0.0;

			String extractionQuality = determineExtractionQuality(confidence);

			// Update cached PDF with extraction results
			String confidenceScore = String.format("%.3f", confidence);
			pdfService.updatePdfExtractionResults(cacheKey, extractedData, confidenceScore, extractionQuality);

			// Finalize: persist PDF to filesystem + DB, get the auto-generated document ID
			UUID documentId = pdfService.finalizePdf(cacheKey);

			// Create activity with extracted data and defaults
			Map<String, Object> activityData = applyActivityDefaults(extractedData);
			activityData.put("documentId", documentId);

			Activity activity = createActivityFromMap(activityData);
			ActivityResponse saved = createActivity(activity);

			Map<String, Object> response = new HashMap<>();
			response.put("activity", saved);
			response.put("documentId", documentId);
			response.put("extractionConfidence", confidence);
			response.put("extractionQuality", extractionQuality);

			return response;
		} catch (Exception e) {
			throw new RuntimeException("Failed to upload and create activity: " + e.getMessage(), e);
		}
	}

	/**
	 * Determine extraction quality based on confidence
	 */
	private String determineExtractionQuality(Double confidence) {
		if (confidence < 0.5) {
			return "low";
		} else if (confidence < 0.75) {
			return "medium";
		}
		return "high";
	}

	/**
	 * Apply default values to activity data
	 */
	private Map<String, Object> applyActivityDefaults(Map<String, Object> extractedData) {
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

	/**
	 * Build recommendation criteria from request parameters
	 */
	public Map<String, Object> buildRecommendationCriteria(String name, Integer targetAge, List<String> format,
			List<String> bloomLevels, Integer targetDuration, List<String> availableResources,
			List<String> preferredTopics, List<String> priorityCategories) {
		Map<String, Object> criteria = new HashMap<>();
		if (name != null)
			criteria.put("name", name);
		if (targetAge != null)
			criteria.put("targetAge", targetAge);
		if (format != null)
			criteria.put("format", format);
		if (bloomLevels != null)
			criteria.put("bloomLevels", bloomLevels);
		if (targetDuration != null)
			criteria.put("targetDuration", targetDuration);
		if (availableResources != null)
			criteria.put("availableResources", availableResources);
		if (preferredTopics != null)
			criteria.put("preferredTopics", preferredTopics);
		if (priorityCategories != null)
			criteria.put("priorityCategories", priorityCategories);
		return criteria;
	}

	/**
	 * Extract and validate breaks for lesson plan
	 */
	public List<Map<String, Object>> processLessonPlanBreaks(List<Map<String, Object>> activities,
			List<Map<String, Object>> breaks) {
		// Extract breaks from request or from activities
		List<Map<String, Object>> processedBreaks = breaks;
		if (processedBreaks == null) {
			processedBreaks = new java.util.ArrayList<>();
			// Extract breaks from activities' break_after field
			for (int i = 0; i < activities.size() - 1; i++) {
				Map<String, Object> activity = activities.get(i);
				if (activity.get("breakAfter") != null) {
					processedBreaks.add((Map<String, Object>) activity.get("breakAfter"));
				}
			}
		}

		// SAFEGUARD: Maximum (n-1) breaks for n activities
		int maxBreaks = Math.max(activities.size() - 1, 0);
		if (processedBreaks.size() > maxBreaks) {
			processedBreaks = processedBreaks.subList(0, maxBreaks);
		}

		return processedBreaks;
	}
}
