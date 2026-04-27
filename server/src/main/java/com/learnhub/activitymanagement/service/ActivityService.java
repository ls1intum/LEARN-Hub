package com.learnhub.activitymanagement.service;

import com.learnhub.activitymanagement.dto.response.ActivityResponse;
import com.learnhub.activitymanagement.dto.response.DocumentResponse;
import com.learnhub.activitymanagement.dto.response.MarkdownResponse;
import com.learnhub.activitymanagement.entity.Activity;
import com.learnhub.activitymanagement.entity.ActivityMarkdown;
import com.learnhub.activitymanagement.entity.enums.*;
import com.learnhub.activitymanagement.repository.ActivityRepository;
import com.learnhub.documentmanagement.entity.PDFDocument;
import com.learnhub.documentmanagement.repository.PDFDocumentRepository;
import com.learnhub.documentmanagement.service.PDFService;
import com.learnhub.exception.ResourceNotFoundException;
import com.learnhub.service.SanitizationService;
import com.learnhub.usermanagement.entity.UserFavourites;
import com.learnhub.usermanagement.repository.UserFavouritesRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	private ActivityExtractionService extractionService;

	@Autowired
	private SanitizationService sanitizationService;

	@Autowired
	private UserFavouritesRepository userFavouritesRepository;

	public long countActivitiesWithFilters(String name, Integer ageMin, Integer ageMax, Integer durationMin,
			Integer durationMax, List<String> formats, List<String> bloomLevels, String mentalLoad,
			String physicalEnergy, List<String> resourcesNeeded, List<String> topics) {
		return getFilteredActivities(name, ageMin, ageMax, durationMin, durationMax, formats, bloomLevels, mentalLoad,
				physicalEnergy, resourcesNeeded, topics).size();
	}

	public List<ActivityResponse> getActivitiesWithFilters(String name, Integer ageMin, Integer ageMax,
			Integer durationMin, Integer durationMax, List<String> formats, List<String> bloomLevels, String mentalLoad,
			String physicalEnergy, List<String> resourcesNeeded, List<String> topics, Integer limit, Integer offset) {
		return getActivitiesWithFilters(name, ageMin, ageMax, durationMin, durationMax, formats, bloomLevels,
				mentalLoad, physicalEnergy, resourcesNeeded, topics, limit, offset, false);
	}

	public List<ActivityResponse> getActivitiesWithFilters(String name, Integer ageMin, Integer ageMax,
			Integer durationMin, Integer durationMax, List<String> formats, List<String> bloomLevels, String mentalLoad,
			String physicalEnergy, List<String> resourcesNeeded, List<String> topics, Integer limit, Integer offset,
			boolean includeSourcePdf) {
		return getActivitiesWithFilters(name, ageMin, ageMax, durationMin, durationMax, formats, bloomLevels,
				mentalLoad, physicalEnergy, resourcesNeeded, topics, limit, offset, includeSourcePdf, null);
	}

	public List<ActivityResponse> getActivitiesWithFilters(String name, Integer ageMin, Integer ageMax,
			Integer durationMin, Integer durationMax, List<String> formats, List<String> bloomLevels, String mentalLoad,
			String physicalEnergy, List<String> resourcesNeeded, List<String> topics, Integer limit, Integer offset,
			boolean includeSourcePdf, UUID userId) {
		List<Activity> allMatching = getFilteredActivities(name, ageMin, ageMax, durationMin, durationMax, formats,
				bloomLevels, mentalLoad, physicalEnergy, resourcesNeeded, topics);

		int start = Math.min((offset != null && offset > 0) ? offset : 0, allMatching.size());
		int pageSize = (limit != null && limit > 0) ? limit : allMatching.size();
		int end = Math.min(start + pageSize, allMatching.size());
		List<Activity> page = allMatching.subList(start, end);
		Set<UUID> favouritedActivityIds = findFavouritedActivityIds(userId, page);

		return page.stream().map(activity -> {
			ActivityResponse response = mapToResponse(activity, includeSourcePdf);
			response.setFavourited(favouritedActivityIds.contains(activity.getId()));
			return response;
		}).collect(Collectors.toList());
	}

	private Set<UUID> findFavouritedActivityIds(UUID userId, List<Activity> activities) {
		if (userId == null || activities.isEmpty()) {
			return Set.of();
		}

		List<UUID> activityIds = activities.stream().map(Activity::getId).filter(Objects::nonNull)
				.collect(Collectors.toList());
		if (activityIds.isEmpty()) {
			return Set.of();
		}

		return userFavouritesRepository.findByUserIdAndFavouriteTypeAndActivityIdIn(userId, "activity", activityIds)
				.stream().map(UserFavourites::getActivityId).filter(Objects::nonNull).collect(Collectors.toSet());
	}

	private List<Activity> getFilteredActivities(String name, Integer ageMin, Integer ageMax, Integer durationMin,
			Integer durationMax, List<String> formats, List<String> bloomLevels, String mentalLoad,
			String physicalEnergy, List<String> resourcesNeeded, List<String> topics) {

		Specification<Activity> spec = Specification.where(null);

		// Only return published activities in normal list/count/recommendation queries
		spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), ActivityStatus.PUBLISHED));

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
		return getActivityById(id, false);
	}

	public ActivityResponse getActivityById(UUID id, boolean includeSourcePdf) {
		return getActivityById(id, includeSourcePdf, false);
	}

	public ActivityResponse getActivityById(UUID id, boolean includeSourcePdf, boolean includeMarkdownContent) {
		logger.debug("Fetching activity by id={}", id);
		Activity activity = activityRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
		return mapToResponse(activity, includeSourcePdf, includeMarkdownContent);
	}

	public List<MarkdownResponse> getActivityMarkdowns(UUID id) {
		logger.debug("Fetching markdowns for activity id={}", id);
		Activity activity = activityRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
		return mapLatestMarkdowns(activity, true);
	}

	public ActivityResponse createActivity(Activity activity) {
		logger.debug("Saving new activity: name={}", activity.getName());
		sanitizeActivity(activity);
		Activity saved = activityRepository.save(activity);
		logger.debug("Activity saved with id={}", saved.getId());
		return mapToResponse(saved, false);
	}

	public ActivityResponse updateActivity(UUID id, Activity activityUpdate) {
		Activity activity = activityRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Activity not found"));

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

		// Update markdowns if provided
		updateMarkdownByType(activity, activityUpdate, MarkdownType.ARTIKULATIONSSCHEMA);
		updateMarkdownByType(activity, activityUpdate, MarkdownType.DECKBLATT);
		updateMarkdownByType(activity, activityUpdate, MarkdownType.HINTERGRUNDWISSEN);
		updateMarkdownByType(activity, activityUpdate, MarkdownType.UEBUNG);
		updateMarkdownByType(activity, activityUpdate, MarkdownType.UEBUNG_LOESUNG);

		sanitizeActivityMetadata(activity);
		Activity saved = activityRepository.save(activity);
		return mapToResponse(saved, false);
	}

	private void updateMarkdownByType(Activity activity, Activity activityUpdate, MarkdownType type) {
		Optional<ActivityMarkdown> newMarkdown = activityUpdate.getMarkdowns().stream().filter(m -> m.getType() == type)
				.findFirst();
		if (newMarkdown.isPresent()) {
			String newContent = sanitizationService.sanitize(newMarkdown.get().getContent());
			boolean newLandscape = newMarkdown.get().isLandscape();
			Optional<ActivityMarkdown> existing = activity.getMarkdowns().stream().filter(m -> m.getType() == type)
					.findFirst();
			if (existing.isPresent()) {
				if (Objects.equals(existing.get().getContent(), newContent)
						&& existing.get().isLandscape() == newLandscape) {
					return;
				}
				existing.get().setContent(newContent);
				existing.get().setLandscape(newLandscape);
			} else {
				ActivityMarkdown md = new ActivityMarkdown();
				md.setActivity(activity);
				md.setType(type);
				md.setContent(newContent);
				md.setLandscape(newLandscape);
				md.setCreatedAt(LocalDateTime.now());
				activity.getMarkdowns().add(md);
			}
		}
	}

	private void sanitizeActivity(Activity activity) {
		if (activity == null) {
			return;
		}

		sanitizeActivityMetadata(activity);

		for (ActivityMarkdown markdown : activity.getMarkdowns()) {
			markdown.setContent(sanitizationService.sanitize(markdown.getContent()));
		}
	}

	private void sanitizeActivityMetadata(Activity activity) {
		if (activity == null) {
			return;
		}

		activity.setName(sanitizationService.sanitize(activity.getName()));
		activity.setDescription(sanitizationService.sanitize(activity.getDescription()));
		activity.setSource(sanitizationService.sanitize(activity.getSource()));
		activity.setResourcesNeeded(sanitizationService.sanitizeList(activity.getResourcesNeeded()));
		activity.setTopics(sanitizationService.sanitizeList(activity.getTopics()));
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
		userFavouritesRepository.deleteByActivityId(id);
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
			actMd.setLandscape(true);
			actMd.setCreatedAt(LocalDateTime.now());
			activity.getMarkdowns().add(actMd);
		}

		if (data.get("deckblattMarkdown") != null) {
			ActivityMarkdown deckblattMd = new ActivityMarkdown();
			deckblattMd.setActivity(activity);
			deckblattMd.setType(MarkdownType.DECKBLATT);
			deckblattMd.setContent(data.get("deckblattMarkdown").toString());
			deckblattMd.setLandscape(false);
			deckblattMd.setCreatedAt(LocalDateTime.now());
			activity.getMarkdowns().add(deckblattMd);
		}

		if (data.get("hintergrundwissenMarkdown") != null) {
			ActivityMarkdown hintergrundwissenMd = new ActivityMarkdown();
			hintergrundwissenMd.setActivity(activity);
			hintergrundwissenMd.setType(MarkdownType.HINTERGRUNDWISSEN);
			hintergrundwissenMd.setContent(data.get("hintergrundwissenMarkdown").toString());
			hintergrundwissenMd.setLandscape(false);
			hintergrundwissenMd.setCreatedAt(LocalDateTime.now());
			activity.getMarkdowns().add(hintergrundwissenMd);
		}

		if (data.get("uebungMarkdown") != null) {
			ActivityMarkdown uebungMd = new ActivityMarkdown();
			uebungMd.setActivity(activity);
			uebungMd.setType(MarkdownType.UEBUNG);
			uebungMd.setContent(data.get("uebungMarkdown").toString());
			uebungMd.setLandscape(false);
			uebungMd.setCreatedAt(LocalDateTime.now());
			activity.getMarkdowns().add(uebungMd);
		}

		if (data.get("uebungLoesungMarkdown") != null) {
			ActivityMarkdown uebungLoesungMd = new ActivityMarkdown();
			uebungLoesungMd.setActivity(activity);
			uebungLoesungMd.setType(MarkdownType.UEBUNG_LOESUNG);
			uebungLoesungMd.setContent(data.get("uebungLoesungMarkdown").toString());
			uebungLoesungMd.setLandscape(false);
			uebungLoesungMd.setCreatedAt(LocalDateTime.now());
			activity.getMarkdowns().add(uebungLoesungMd);
		}

		return activity;
	}

	public ActivityResponse convertToResponse(Activity activity) {
		return convertToResponse(activity, false);
	}

	public ActivityResponse convertToResponse(Activity activity, boolean includeSourcePdf) {
		return mapToResponse(activity, includeSourcePdf, false);
	}

	public ActivityResponse convertToResponse(Activity activity, boolean includeSourcePdf,
			boolean includeMarkdownContent) {
		return mapToResponse(activity, includeSourcePdf, includeMarkdownContent);
	}

	private ActivityResponse mapToResponse(Activity activity, boolean includeSourcePdf) {
		return mapToResponse(activity, includeSourcePdf, false);
	}

	private ActivityResponse mapToResponse(Activity activity, boolean includeSourcePdf,
			boolean includeMarkdownContent) {
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

		List<DocumentResponse> docResponses = activity.getDocuments().stream()
				.filter(document -> includeSourcePdf || document.getType() != DocumentType.SOURCE_PDF)
				.map(d -> new DocumentResponse(d.getId(), d.getFilename(), d.getFileSize(),
						d.getType() != null ? d.getType().getValue() : null))
				.collect(Collectors.toList());
		response.setDocuments(docResponses);

		response.setMarkdowns(mapLatestMarkdowns(activity, includeMarkdownContent));
		response.setStatus(activity.getStatus() != null ? activity.getStatus().name() : ActivityStatus.PUBLISHED.name());
		response.setGenerationError(activity.getGenerationError());

		return response;
	}

	private List<MarkdownResponse> mapLatestMarkdowns(Activity activity, boolean includeContent) {
		Map<String, ActivityMarkdown> latestMarkdownByType = activity.getMarkdowns().stream()
				.filter(m -> m.getType() != null)
				.sorted(Comparator
						.comparing(ActivityMarkdown::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
						.reversed())
				.collect(Collectors.toMap(m -> m.getType().getValue(), m -> m, (existing, ignored) -> existing,
						LinkedHashMap::new));
		return latestMarkdownByType.values().stream().map(m -> new MarkdownResponse(m.getId(), m.getType().getValue(),
				includeContent ? m.getContent() : null, m.isLandscape())).collect(Collectors.toList());
	}

	/**
	 * Extract metadata from a cached or persisted PDF. Delegates to
	 * {@link ActivityExtractionService}.
	 */
	public Map<String, Object> extractMetadataFromDocument(UUID documentIdOrCacheKey) {
		return extractionService.extractMetadataFromDocument(documentIdOrCacheKey);
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

	// ---- Draft / Publish workflow ----

	public List<ActivityResponse> getDraftActivities() {
		List<Activity> drafts = activityRepository
				.findByStatusInOrderByCreatedAtDesc(List.of(ActivityStatus.PENDING, ActivityStatus.DRAFT));
		return drafts.stream().map(a -> mapToResponse(a, true)).collect(Collectors.toList());
	}

	@Transactional
	public ActivityResponse publishActivity(UUID id) {
		Activity activity = activityRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
		if (activity.getStatus() != ActivityStatus.DRAFT) {
			throw new IllegalStateException("Only DRAFT activities can be published");
		}
		activity.setStatus(ActivityStatus.PUBLISHED);
		return mapToResponse(activityRepository.save(activity), false);
	}

	@Transactional
	public void updateActivityWithMetadata(UUID id, Map<String, Object> extractedData) {
		Activity activity = activityRepository.findById(id).orElse(null);
		if (activity == null) return;

		if (extractedData.get("name") != null) activity.setName(extractedData.get("name").toString());
		if (extractedData.get("description") != null) activity.setDescription(extractedData.get("description").toString());
		if (extractedData.get("source") != null) activity.setSource(extractedData.get("source").toString());
		if (extractedData.get("ageMin") != null) activity.setAgeMin(Integer.parseInt(extractedData.get("ageMin").toString()));
		if (extractedData.get("ageMax") != null) activity.setAgeMax(Integer.parseInt(extractedData.get("ageMax").toString()));
		if (extractedData.get("format") != null) {
			try { activity.setFormat(ActivityFormat.fromValue(extractedData.get("format").toString())); } catch (Exception ignored) {}
		}
		if (extractedData.get("bloomLevel") != null) {
			try { activity.setBloomLevel(BloomLevel.fromValue(extractedData.get("bloomLevel").toString())); } catch (Exception ignored) {}
		}
		if (extractedData.get("durationMinMinutes") != null) activity.setDurationMinMinutes(Integer.parseInt(extractedData.get("durationMinMinutes").toString()));
		if (extractedData.get("durationMaxMinutes") != null) activity.setDurationMaxMinutes(Integer.parseInt(extractedData.get("durationMaxMinutes").toString()));
		if (extractedData.get("mentalLoad") != null) {
			try { activity.setMentalLoad(EnergyLevel.fromValue(extractedData.get("mentalLoad").toString())); } catch (Exception ignored) {}
		}
		if (extractedData.get("physicalEnergy") != null) {
			try { activity.setPhysicalEnergy(EnergyLevel.fromValue(extractedData.get("physicalEnergy").toString())); } catch (Exception ignored) {}
		}
		if (extractedData.get("prepTimeMinutes") != null) activity.setPrepTimeMinutes(Integer.parseInt(extractedData.get("prepTimeMinutes").toString()));
		if (extractedData.get("cleanupTimeMinutes") != null) activity.setCleanupTimeMinutes(Integer.parseInt(extractedData.get("cleanupTimeMinutes").toString()));
		if (extractedData.get("resourcesNeeded") instanceof List) {
			@SuppressWarnings("unchecked") List<String> r = (List<String>) extractedData.get("resourcesNeeded");
			activity.setResourcesNeeded(r);
		}
		if (extractedData.get("topics") instanceof List) {
			@SuppressWarnings("unchecked") List<String> t = (List<String>) extractedData.get("topics");
			activity.setTopics(t);
		}

		sanitizeActivityMetadata(activity);
		activityRepository.save(activity);
	}

	@Transactional
	public void addMarkdownsToActivity(UUID id, Map<String, String> markdownsByType) {
		Activity activity = activityRepository.findById(id).orElse(null);
		if (activity == null) return;

		Map<String, Boolean> landscapes = Map.of(
			"deckblatt", false,
			"artikulationsschema", true,
			"hintergrundwissen", false,
			"uebung", false,
			"uebung_loesung", false
		);

		for (Map.Entry<String, String> entry : markdownsByType.entrySet()) {
			if (entry.getValue() == null) continue;
			try {
				MarkdownType type = MarkdownType.fromValue(entry.getKey());
				boolean landscape = Boolean.TRUE.equals(landscapes.get(entry.getKey()));
				String content = sanitizationService.sanitize(entry.getValue());
				Optional<ActivityMarkdown> existing = activity.getMarkdowns().stream()
						.filter(m -> m.getType() == type).findFirst();
				if (existing.isPresent()) {
					existing.get().setContent(content);
					existing.get().setLandscape(landscape);
				} else {
					ActivityMarkdown md = new ActivityMarkdown();
					md.setActivity(activity);
					md.setType(type);
					md.setContent(content);
					md.setLandscape(landscape);
					md.setCreatedAt(LocalDateTime.now());
					activity.getMarkdowns().add(md);
				}
			} catch (Exception ignored) {}
		}
		activityRepository.save(activity);
	}

	@Transactional
	public void setActivityStatus(UUID id, ActivityStatus status) {
		Activity activity = activityRepository.findById(id).orElse(null);
		if (activity == null) return;
		activity.setStatus(status);
		activityRepository.save(activity);
	}

	@Transactional
	public void setActivityGenerationError(UUID id, String error) {
		Activity activity = activityRepository.findById(id).orElse(null);
		if (activity == null) return;
		activity.setGenerationError(error);
		activityRepository.save(activity);
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
