package com.learnhub.activitymanagement.service;

import com.learnhub.activitymanagement.dto.response.ActivityResponse;
import com.learnhub.activitymanagement.dto.response.CategoryScoreResponse;
import com.learnhub.activitymanagement.dto.response.ScoreResponse;
import com.learnhub.activitymanagement.entity.Activity;
import com.learnhub.activitymanagement.entity.Break;
import com.learnhub.activitymanagement.entity.enums.ActivityFormat;
import com.learnhub.activitymanagement.entity.enums.ActivityResource;
import com.learnhub.activitymanagement.entity.enums.BloomLevel;
import com.learnhub.activitymanagement.repository.ActivityRepository;
import com.learnhub.activitymanagement.service.ScoringEngine.SearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ActivityService activityService;

    public Map<String, Object> getRecommendations(
            Map<String, Object> criteriaMap,
            boolean includeBreaks,
            int maxActivityCount,
            int limit) {

        try {
            // Convert criteria map to SearchCriteria
            SearchCriteria criteria = convertCriteria(criteriaMap);
            List<String> priorityCategories = extractPriorityCategories(criteriaMap);

            // Load all activities
            List<Activity> activities = activityRepository.findAll();

            // Filter activities based on hard constraints
            List<Activity> filteredActivities = filterActivities(activities, criteria);

            // Create scoring engine
            ScoringEngine scoringEngine = new ScoringEngine(priorityCategories);

            // Generate recommendations
            List<Map<String, Object>> recommendations = new ArrayList<>();

            if (maxActivityCount == 1) {
                // Single activity recommendations
                for (Activity activity : filteredActivities) {
                    ScoreResponse score = scoringEngine.scoreActivity(activity, criteria);

                    Map<String, Object> recommendation = new HashMap<>();
                    recommendation.put("activities", Arrays.asList(convertToResponse(activity)));
                    recommendation.put("score", score.getTotalScore());
                    recommendation.put("score_breakdown", score.getCategoryScores());

                    recommendations.add(recommendation);
                }
            } else {
                // Multi-activity recommendations (sequences/lesson plans)
                List<List<Activity>> sequences = generateSequences(filteredActivities, maxActivityCount);

                for (List<Activity> sequence : sequences) {
                    ScoreResponse score = scoringEngine.scoreSequence(sequence, criteria);

                    // Add breaks if requested
                    if (includeBreaks) {
                        addBreaksToSequence(sequence, criteria);
                    }

                    List<Map<String, Object>> activityResponses = sequence.stream()
                            .map(this::convertToResponse)
                            .collect(Collectors.toList());

                    Map<String, Object> recommendation = new HashMap<>();
                    recommendation.put("activities", activityResponses);
                    recommendation.put("score", score.getTotalScore());
                    recommendation.put("score_breakdown", score.getCategoryScores());

                    recommendations.add(recommendation);
                }
            }

            // Sort by score descending
            recommendations.sort((a, b) -> {
                int scoreA = (int) a.get("score");
                int scoreB = (int) b.get("score");
                return Integer.compare(scoreB, scoreA);
            });

            // Limit results
            if (recommendations.size() > limit) {
                recommendations = recommendations.subList(0, limit);
            }

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("activities", recommendations);
            response.put("total", recommendations.size());
            response.put("search_criteria", criteriaMap);
            response.put("generated_at", Instant.now().toString());

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("activities", new ArrayList<>());
            response.put("total", 0);
            response.put("search_criteria", criteriaMap);
            response.put("generated_at", Instant.now().toString());
            return response;
        }
    }

    private SearchCriteria convertCriteria(Map<String, Object> criteriaMap) {
        SearchCriteria criteria = new SearchCriteria();

        if (criteriaMap.containsKey("name")) {
            criteria.setName((String) criteriaMap.get("name"));
        }

        if (criteriaMap.containsKey("target_age")) {
            criteria.setTargetAge(toInt(criteriaMap.get("target_age")));
        }

        if (criteriaMap.containsKey("format")) {
            criteria.setFormats(toEnumList(criteriaMap.get("format"), ActivityFormat.class));
        }

        if (criteriaMap.containsKey("bloom_levels")) {
            criteria.setBloomLevels(toEnumList(criteriaMap.get("bloom_levels"), BloomLevel.class));
        }

        if (criteriaMap.containsKey("target_duration")) {
            criteria.setTargetDuration(toInt(criteriaMap.get("target_duration")));
        }

        if (criteriaMap.containsKey("available_resources")) {
            criteria.setAvailableResources(toEnumList(criteriaMap.get("available_resources"), ActivityResource.class));
        }

        if (criteriaMap.containsKey("preferred_topics")) {
            criteria.setPreferredTopics(toStringList(criteriaMap.get("preferred_topics")));
        }

        return criteria;
    }

    private List<String> extractPriorityCategories(Map<String, Object> criteriaMap) {
        Object priorityObj = criteriaMap.get("priority_categories");
        if (priorityObj == null) {
            return new ArrayList<>();
        }

        if (priorityObj instanceof List) {
            return ((List<?>) priorityObj).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    private List<Activity> filterActivities(List<Activity> activities, SearchCriteria criteria) {
        return activities.stream()
                .filter(activity -> matchesCriteria(activity, criteria))
                .collect(Collectors.toList());
    }

    private boolean matchesCriteria(Activity activity, SearchCriteria criteria) {
        // Name filter
        if (criteria.getName() != null && !criteria.getName().isEmpty()) {
            if (!activity.getName().toLowerCase().contains(criteria.getName().toLowerCase())) {
                return false;
            }
        }

        // Format filter
        if (criteria.getFormats() != null && !criteria.getFormats().isEmpty()) {
            if (!criteria.getFormats().contains(activity.getFormat())) {
                return false;
            }
        }

        // Bloom level filter
        if (criteria.getBloomLevels() != null && !criteria.getBloomLevels().isEmpty()) {
            if (!criteria.getBloomLevels().contains(activity.getBloomLevel())) {
                return false;
            }
        }

        // Resources filter
        if (criteria.getAvailableResources() != null && !criteria.getAvailableResources().isEmpty()) {
            List<String> activityResources = activity.getResourcesNeeded();
            if (activityResources != null && !activityResources.isEmpty()) {
                Set<String> availableResources = criteria.getAvailableResources().stream()
                        .filter(Objects::nonNull)
                        .map(r -> r.getValue().toLowerCase())
                        .collect(Collectors.toSet());

                boolean hasAllResources = activityResources.stream()
                        .filter(Objects::nonNull)
                        .map(String::toLowerCase)
                        .allMatch(availableResources::contains);
                if (!hasAllResources) {
                    return false;
                }
            }
        }

        // Topics filter
        if (criteria.getPreferredTopics() != null && !criteria.getPreferredTopics().isEmpty()) {
            List<String> activityTopics = activity.getTopics();
            if (activityTopics == null || activityTopics.isEmpty()) {
                return false;
            }

            boolean hasAnyTopic = activityTopics.stream()
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .anyMatch(topic -> criteria.getPreferredTopics().stream()
                            .filter(Objects::nonNull)
                            .map(String::toLowerCase)
                            .anyMatch(pref -> pref.equals(topic)));
            if (!hasAnyTopic) {
                return false;
            }
        }

        return true;
    }

    private List<List<Activity>> generateSequences(List<Activity> activities, int maxActivityCount) {
        List<List<Activity>> sequences = new ArrayList<>();

        // Generate all combinations up to maxActivityCount
        for (int i = 1; i <= maxActivityCount && i <= activities.size(); i++) {
            generateCombinations(activities, i, sequences);
        }

        return sequences;
    }

    private void generateCombinations(List<Activity> activities, int size, List<List<Activity>> result) {
        if (size == 1) {
            for (Activity activity : activities) {
                result.add(Arrays.asList(activity));
            }
            return;
        }

        // For size > 1, generate combinations
        // Limit to prevent combinatorial explosion
        int maxCombinations = 100;
        for (int i = 0; i <= activities.size() - size && result.size() < maxCombinations; i++) {
            for (int j = i + 1; j <= activities.size() - size + 1 && result.size() < maxCombinations; j++) {
                List<Activity> combination = new ArrayList<>();
                combination.add(activities.get(i));
                combination.add(activities.get(j));

                if (size > 2) {
                    for (int k = j + 1; k < activities.size() && combination.size() < size; k++) {
                        combination.add(activities.get(k));
                    }
                }

                if (combination.size() == size) {
                    result.add(combination);
                }
            }
        }
    }

    private void addBreaksToSequence(List<Activity> sequence, SearchCriteria criteria) {
        // Add break_after to activities where appropriate
        for (int i = 0; i < sequence.size() - 1; i++) {
            Activity activity = sequence.get(i);
            Activity nextActivity = sequence.get(i + 1);

            // Calculate recommended break duration (simple heuristic)
            int breakDuration = calculateBreakDuration(activity, nextActivity);

            if (breakDuration > 0) {
                Break breakInfo = new Break(
                        "break-" + activity.getId(),
                        breakDuration,
                        "Break between activities",
                        Arrays.asList("energy_level_change", "topic_transition"));
                activity.setBreakAfter(breakInfo);
            }
        }
    }

    private int calculateBreakDuration(Activity current, Activity next) {
        // Simple heuristic: 5 minutes for topic changes, 10 minutes for energy level
        // changes
        int breakDuration = 0;

        // Check topic change
        Set<String> currentTopics = current.getTopics() != null
                ? current.getTopics().stream().filter(Objects::nonNull).map(String::toLowerCase)
                        .collect(Collectors.toSet())
                : new HashSet<>();
        Set<String> nextTopics = next.getTopics() != null
                ? next.getTopics().stream().filter(Objects::nonNull).map(String::toLowerCase)
                        .collect(Collectors.toSet())
                : new HashSet<>();
        currentTopics.retainAll(nextTopics);
        if (currentTopics.isEmpty()) {
            breakDuration += 5;
        }

        // Check energy level change
        if (current.getPhysicalEnergy() != null && next.getPhysicalEnergy() != null) {
            if (!current.getPhysicalEnergy().equals(next.getPhysicalEnergy())) {
                breakDuration += 5;
            }
        }

        return Math.min(breakDuration, 10); // Cap at 10 minutes
    }

    private Map<String, Object> convertToResponse(Activity activity) {
        ActivityResponse response = activityService.convertToResponse(activity);
        Map<String, Object> map = new HashMap<>();
        map.put("id", response.getId());
        map.put("name", response.getName());
        map.put("description", response.getDescription());
        map.put("source", response.getSource());
        map.put("age_min", response.getAgeMin());
        map.put("age_max", response.getAgeMax());
        map.put("format", response.getFormat());
        map.put("bloom_level", response.getBloomLevel());
        map.put("duration_min_minutes", response.getDurationMinMinutes());
        map.put("duration_max_minutes", response.getDurationMaxMinutes());
        map.put("mental_load", response.getMentalLoad());
        map.put("physical_energy", response.getPhysicalEnergy());
        map.put("prep_time_minutes", response.getPrepTimeMinutes());
        map.put("cleanup_time_minutes", response.getCleanupTimeMinutes());
        map.put("resources_needed", response.getResourcesNeeded());
        map.put("topics", response.getTopics());
        map.put("document_id", response.getDocumentId());
        map.put("type", "activity");

        if (activity.getBreakAfter() != null) {
            Map<String, Object> breakMap = new HashMap<>();
            breakMap.put("id", activity.getBreakAfter().getId());
            breakMap.put("duration", activity.getBreakAfter().getDuration());
            breakMap.put("description", activity.getBreakAfter().getDescription());
            breakMap.put("reasons", activity.getBreakAfter().getReasons());
            map.put("break_after", breakMap);
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
