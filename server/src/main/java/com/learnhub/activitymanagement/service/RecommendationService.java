package com.learnhub.activitymanagement.service;

import com.learnhub.activitymanagement.dto.response.ActivityResponse;
import com.learnhub.activitymanagement.dto.response.ScoreResponse;
import com.learnhub.activitymanagement.entity.Activity;
import com.learnhub.activitymanagement.entity.Break;
import com.learnhub.activitymanagement.entity.enums.ActivityFormat;
import com.learnhub.activitymanagement.entity.enums.ActivityResource;
import com.learnhub.activitymanagement.entity.enums.BloomLevel;
import com.learnhub.activitymanagement.entity.enums.EnergyLevel;
import com.learnhub.activitymanagement.repository.ActivityRepository;
import com.learnhub.activitymanagement.service.ScoringEngineService.SearchCriteria;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

	private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);

	// Matches Flask's AGE_FILTER_TOLERANCE = 2
	private static final int AGE_FILTER_TOLERANCE = 2;

	// Matches Flask's LESSON_PLAN_TOP_ACTIVITIES_LIMIT = 25
	private static final int LESSON_PLAN_TOP_ACTIVITIES_LIMIT = 25;

	// Upper bound (exclusive) for lesson plan combination size: k ranges from 2 to
	// MAX_LESSON_PLAN_SIZE-1
	// Matches Flask's range(2, min(max_activity_count + 1, 6))
	private static final int MAX_LESSON_PLAN_SIZE = 6;

	@Autowired
	private ActivityRepository activityRepository;

	@Autowired
	private ActivityService activityService;

	public Map<String, Object> getRecommendations(Map<String, Object> criteriaMap, boolean includeBreaks,
			int maxActivityCount, int limit) {

		try {
			// Convert criteria map to SearchCriteria
			SearchCriteria criteria = convertCriteria(criteriaMap);
			List<String> priorityCategories = extractPriorityCategories(criteriaMap);

			// Load all activities
			List<Activity> activities = activityRepository.findAll();

			// Filter activities based on hard constraints
			List<Activity> filteredActivities = filterActivities(activities, criteria);

			// Create scoring engine
			ScoringEngineService scoringEngine = new ScoringEngineService(priorityCategories);

			// Phase 1: Score all activities without duration
			// Each entry is a pair: [List<Activity>, ScoreResponse]
			List<Object[]> results = new ArrayList<>();

			for (Activity activity : filteredActivities) {
				ScoreResponse score = scoringEngine.scoreActivityWithoutDuration(activity, criteria);
				results.add(new Object[]{Collections.singletonList(activity), score});
			}

			// Generate lesson plans if maxActivityCount > 1
			if (maxActivityCount > 1 && filteredActivities.size() > 1) {
				// Take first LESSON_PLAN_TOP_ACTIVITIES_LIMIT activities for lesson plan
				// generation
				List<Activity> topActivities = filteredActivities.stream().limit(LESSON_PLAN_TOP_ACTIVITIES_LIMIT)
						.collect(Collectors.toList());

				// Generate combinations of k = 2 to min(maxActivityCount, 5) inclusive
				for (int k = 2; k < Math.min(maxActivityCount + 1, MAX_LESSON_PLAN_SIZE); k++) {
					if (k <= topActivities.size()) {
						List<List<Activity>> combos = new ArrayList<>();
						generateCombinations(topActivities, k, combos);
						for (List<Activity> combo : combos) {
							ScoreResponse score = scoringEngine.scoreSequenceWithoutDuration(combo, criteria);
							results.add(new Object[]{combo, score});
						}
					}
				}
			}

			// Sort by pre-duration score descending
			results.sort((a, b) -> {
				int scoreA = ((ScoreResponse) a[1]).getTotalScore();
				int scoreB = ((ScoreResponse) b[1]).getTotalScore();
				return Integer.compare(scoreB, scoreA);
			});

			// Add breaks to multi-activity results if requested
			if (includeBreaks) {
				for (Object[] result : results) {
					@SuppressWarnings("unchecked")
					List<Activity> activityList = (List<Activity>) result[0];
					if (activityList.size() > 1) {
						assignBreaksToActivities(activityList);
					}
				}
			}

			// Phase 2: Re-score with duration and re-rank
			List<Object[]> rescored = new ArrayList<>();
			for (Object[] result : results) {
				@SuppressWarnings("unchecked")
				List<Activity> activityList = (List<Activity>) result[0];
				ScoreResponse newScore;
				if (activityList.size() == 1) {
					newScore = scoringEngine.scoreActivity(activityList.get(0), criteria);
				} else {
					newScore = scoringEngine.scoreSequence(activityList, criteria);
				}
				rescored.add(new Object[]{activityList, newScore});
			}

			rescored.sort((a, b) -> {
				int scoreA = ((ScoreResponse) a[1]).getTotalScore();
				int scoreB = ((ScoreResponse) b[1]).getTotalScore();
				return Integer.compare(scoreB, scoreA);
			});

			// Apply limit
			if (rescored.size() > limit) {
				rescored = rescored.subList(0, limit);
			}

			// Build response
			List<Map<String, Object>> recommendations = new ArrayList<>();
			for (Object[] result : rescored) {
				@SuppressWarnings("unchecked")
				List<Activity> activityList = (List<Activity>) result[0];
				ScoreResponse score = (ScoreResponse) result[1];

				List<Map<String, Object>> activityResponses = activityList.stream().map(this::convertToResponse)
						.collect(Collectors.toList());

				Map<String, Object> recommendation = new HashMap<>();
				recommendation.put("activities", activityResponses);
				recommendation.put("score", score.getTotalScore());
				recommendation.put("scoreBreakdown", score.getCategoryScores());

				recommendations.add(recommendation);
			}

			Map<String, Object> response = new HashMap<>();
			response.put("activities", recommendations);
			response.put("total", recommendations.size());
			response.put("searchCriteria", criteriaMap);
			response.put("generatedAt", Instant.now().toString());

			return response;

		} catch (Exception e) {
			logger.error("Failed to generate recommendations: {}", e.getMessage(), e);
			Map<String, Object> response = new HashMap<>();
			response.put("activities", new ArrayList<>());
			response.put("total", 0);
			response.put("searchCriteria", criteriaMap);
			response.put("generatedAt", Instant.now().toString());
			return response;
		}
	}

	private SearchCriteria convertCriteria(Map<String, Object> criteriaMap) {
		SearchCriteria criteria = new SearchCriteria();

		if (criteriaMap.containsKey("targetAge")) {
			criteria.setTargetAge(toInt(criteriaMap.get("targetAge")));
		}

		if (criteriaMap.containsKey("format")) {
			criteria.setFormats(toEnumList(criteriaMap.get("format"), ActivityFormat.class));
		}

		if (criteriaMap.containsKey("bloomLevels")) {
			criteria.setBloomLevels(toEnumList(criteriaMap.get("bloomLevels"), BloomLevel.class));
		}

		if (criteriaMap.containsKey("targetDuration")) {
			criteria.setTargetDuration(toInt(criteriaMap.get("targetDuration")));
		}

		if (criteriaMap.containsKey("availableResources")) {
			criteria.setAvailableResources(toEnumList(criteriaMap.get("availableResources"), ActivityResource.class));
		}

		if (criteriaMap.containsKey("preferredTopics")) {
			criteria.setPreferredTopics(toStringList(criteriaMap.get("preferredTopics")));
		}

		return criteria;
	}

	private List<String> extractPriorityCategories(Map<String, Object> criteriaMap) {
		Object priorityObj = criteriaMap.get("priorityCategories");
		if (priorityObj == null) {
			return new ArrayList<>();
		}

		if (priorityObj instanceof List) {
			return ((List<?>) priorityObj).stream().map(Object::toString).collect(Collectors.toList());
		}

		return new ArrayList<>();
	}

	private List<Activity> filterActivities(List<Activity> activities, SearchCriteria criteria) {
		// Precompute the preferred topic set once to avoid recreating it for every
		// activity
		Set<String> preferredTopicSet = (criteria.getPreferredTopics() != null)
				? criteria.getPreferredTopics().stream().filter(Objects::nonNull).map(String::toLowerCase)
						.collect(Collectors.toSet())
				: Collections.emptySet();

		return activities.stream().filter(activity -> matchesCriteria(activity, criteria, preferredTopicSet))
				.collect(Collectors.toList());
	}

	private boolean matchesCriteria(Activity activity, SearchCriteria criteria, Set<String> preferredTopicSet) {
		// Age filter with tolerance (matches Flask's AGE_FILTER_TOLERANCE = 2)
		if (criteria.getTargetAge() != null) {
			if (activity.getAgeMin() == null || activity.getAgeMax() == null) {
				return false;
			}
			boolean ageMinOk = activity.getAgeMin() <= criteria.getTargetAge() + AGE_FILTER_TOLERANCE;
			boolean ageMaxOk = activity.getAgeMax() >= criteria.getTargetAge() - AGE_FILTER_TOLERANCE;
			if (!(ageMinOk && ageMaxOk)) {
				return false;
			}
		}

		// Format filter
		if (criteria.getFormats() != null && !criteria.getFormats().isEmpty()) {
			if (!criteria.getFormats().contains(activity.getFormat())) {
				return false;
			}
		}

		// Duration filter: exclude activities whose minimum duration exceeds the target
		if (criteria.getTargetDuration() != null) {
			if (activity.getDurationMinMinutes() != null
					&& activity.getDurationMinMinutes() > criteria.getTargetDuration()) {
				return false;
			}
		}

		// Resources filter: all required resources must be available
		if (criteria.getAvailableResources() != null && !criteria.getAvailableResources().isEmpty()) {
			List<String> activityResources = activity.getResourcesNeeded();
			if (activityResources != null && !activityResources.isEmpty()) {
				Set<String> availableResources = criteria.getAvailableResources().stream().filter(Objects::nonNull)
						.map(r -> r.getValue().toLowerCase()).collect(Collectors.toSet());

				boolean hasAllResources = activityResources.stream().filter(Objects::nonNull).map(String::toLowerCase)
						.allMatch(availableResources::contains);
				if (!hasAllResources) {
					return false;
				}
			}
		}

		// Bloom level filter (exact match only)
		if (criteria.getBloomLevels() != null && !criteria.getBloomLevels().isEmpty()) {
			if (!criteria.getBloomLevels().contains(activity.getBloomLevel())) {
				return false;
			}
		}

		// Topics filter: at least one activity topic must match a preferred topic
		if (!preferredTopicSet.isEmpty()) {
			List<String> activityTopics = activity.getTopics();
			if (activityTopics == null || activityTopics.isEmpty()) {
				return false;
			}

			boolean hasAnyTopic = activityTopics.stream().filter(Objects::nonNull).map(String::toLowerCase)
					.anyMatch(preferredTopicSet::contains);
			if (!hasAnyTopic) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Generate all combinations of exactly {@code k} activities from the given
	 * list. Equivalent to Python's itertools.combinations(activities, k).
	 */
	private void generateCombinations(List<Activity> activities, int k, List<List<Activity>> result) {
		int n = activities.size();
		if (k > n || k <= 0) {
			return;
		}

		int[] indices = new int[k];
		for (int i = 0; i < k; i++) {
			indices[i] = i;
		}

		while (true) {
			List<Activity> combo = new ArrayList<>(k);
			for (int idx : indices) {
				combo.add(activities.get(idx));
			}
			result.add(combo);

			// Find rightmost index that can be incremented
			int i = k - 1;
			while (i >= 0 && indices[i] == i + n - k) {
				i--;
			}
			if (i < 0) {
				break;
			}
			indices[i]++;
			for (int j = i + 1; j < k; j++) {
				indices[j] = indices[j - 1] + 1;
			}
		}
	}

	/**
	 * Assign breaks between activities in a multi-activity sequence. Matches
	 * Flask's _assign_breaks_to_activities logic exactly: - cleanup_time_minutes of
	 * the current activity - 10 min for HIGH mental load - 5 min for HIGH physical
	 * energy - 5 min for a format change to the next activity Duration is rounded
	 * up to the nearest 5-minute increment. No break is assigned after the last
	 * activity.
	 */
	private void assignBreaksToActivities(List<Activity> activities) {
		if (activities.size() <= 1) {
			return;
		}

		for (int i = 0; i < activities.size() - 1; i++) {
			Activity activity = activities.get(i);
			Activity nextActivity = activities.get(i + 1);

			List<String> breakReasons = new ArrayList<>();
			int breakDuration = 0;

			// Cleanup time at end of activity
			if (activity.getCleanupTimeMinutes() != null && activity.getCleanupTimeMinutes() > 0) {
				breakDuration += activity.getCleanupTimeMinutes();
				breakReasons.add("Cleanup time for " + activity.getName());
			}

			// Mental rest break for high mental load
			if (EnergyLevel.HIGH.equals(activity.getMentalLoad())) {
				breakDuration += 10;
				breakReasons.add("Mental rest break after high cognitive load");
			}

			// Physical rest break for high physical energy
			if (EnergyLevel.HIGH.equals(activity.getPhysicalEnergy())) {
				breakDuration += 5;
				breakReasons.add("Physical rest break after high energy activity");
			}

			// Transition break for format change
			if (activity.getFormat() != null && nextActivity.getFormat() != null
					&& !activity.getFormat().equals(nextActivity.getFormat())) {
				breakDuration += 5;
				breakReasons.add("Transition break from " + activity.getFormat().getValue() + " to "
						+ nextActivity.getFormat().getValue());
			}

			// Only assign a break when there are reasons and a positive duration
			if (!breakReasons.isEmpty() && breakDuration > 0) {
				int roundedDuration = roundUpToNearest5Minutes(breakDuration);
				Break breakInfo = new Break(null, roundedDuration, String.join("; ", breakReasons), breakReasons);
				activity.setBreakAfter(breakInfo);
			}
		}
	}

	/**
	 * Round up a duration (in minutes) to the nearest 5-minute increment. Matches
	 * Flask's round_up_to_nearest_5_minutes utility.
	 */
	private int roundUpToNearest5Minutes(int duration) {
		if (duration <= 0) {
			return 0;
		}
		return ((duration - 1) / 5 + 1) * 5;
	}

	private Map<String, Object> convertToResponse(Activity activity) {
		ActivityResponse response = activityService.convertToResponse(activity);
		Map<String, Object> map = new HashMap<>();
		map.put("id", response.getId());
		map.put("name", response.getName());
		map.put("description", response.getDescription());
		map.put("source", response.getSource());
		map.put("ageMin", response.getAgeMin());
		map.put("ageMax", response.getAgeMax());
		map.put("format", response.getFormat());
		map.put("bloomLevel", response.getBloomLevel());
		map.put("durationMinMinutes", response.getDurationMinMinutes());
		map.put("durationMaxMinutes", response.getDurationMaxMinutes());
		map.put("mentalLoad", response.getMentalLoad());
		map.put("physicalEnergy", response.getPhysicalEnergy());
		map.put("prepTimeMinutes", response.getPrepTimeMinutes());
		map.put("cleanupTimeMinutes", response.getCleanupTimeMinutes());
		map.put("resourcesNeeded", response.getResourcesNeeded());
		map.put("topics", response.getTopics());
		map.put("documents", response.getDocuments());
		map.put("markdowns", response.getMarkdowns());
		map.put("type", "activity");

		if (activity.getBreakAfter() != null) {
			Map<String, Object> breakMap = new HashMap<>();
			breakMap.put("id", activity.getBreakAfter().getId());
			breakMap.put("duration", activity.getBreakAfter().getDuration());
			breakMap.put("description", activity.getBreakAfter().getDescription());
			breakMap.put("reasons", activity.getBreakAfter().getReasons());
			map.put("breakAfter", breakMap);
		}

		return map;
	}

	private Integer toInt(Object obj) {
		if (obj == null)
			return null;
		if (obj instanceof Integer)
			return (Integer) obj;
		try {
			return Integer.parseInt(obj.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private <E extends Enum<E>> List<E> toEnumList(Object obj, Class<E> enumClass) {
		if (obj == null)
			return null;

		List<String> strings = new ArrayList<>();
		if (obj instanceof List) {
			for (Object item : (List<?>) obj) {
				strings.add(item.toString());
			}
		} else if (obj instanceof String) {
			strings.add((String) obj);
		}

		List<E> result = new ArrayList<>();
		for (String str : strings) {
			try {
				result.add(Enum.valueOf(enumClass, str.toUpperCase()));
			} catch (IllegalArgumentException e) {
				// Skip invalid enum values
			}
		}

		return result.isEmpty() ? null : result;
	}

	@SuppressWarnings("unchecked")
	private List<String> toStringList(Object obj) {
		if (obj == null)
			return null;

		List<String> result = new ArrayList<>();
		if (obj instanceof List) {
			for (Object item : (List<Object>) obj) {
				if (item != null) {
					result.add(item.toString());
				}
			}
		} else {
			result.add(obj.toString());
		}

		return result.isEmpty() ? null : result;
	}
}
