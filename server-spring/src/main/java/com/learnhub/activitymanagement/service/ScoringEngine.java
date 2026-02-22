package com.learnhub.activitymanagement.service;

import com.learnhub.activitymanagement.dto.response.CategoryScoreResponse;
import com.learnhub.activitymanagement.dto.response.ScoreResponse;
import com.learnhub.activitymanagement.entity.Activity;
import com.learnhub.activitymanagement.entity.enums.ActivityFormat;
import com.learnhub.activitymanagement.entity.enums.ActivityResource;
import com.learnhub.activitymanagement.entity.enums.BloomLevel;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScoringEngine {

    // Scoring constants
    private static final int AGE_MAX_DISTANCE = 5;
    private static final int BLOOM_ADJACENT_LEVELS = 1;
    private static final double PRIORITY_CATEGORY_MULTIPLIER = 2.0;
    private static final List<String> BLOOM_ORDER = Arrays.asList(
            "Remember", "Understand", "Apply", "Analyze", "Evaluate", "Create");

    // Scoring categories with impact levels
    private static final Map<String, ScoringCategory> SCORING_CATEGORIES = new HashMap<>();

    static {
        SCORING_CATEGORIES.put("age_appropriateness", new ScoringCategory("age_appropriateness", 4,
                "How well the activity matches the target age range"));
        SCORING_CATEGORIES.put("bloom_level_match", new ScoringCategory("bloom_level_match", 5,
                "How well the activity matches the target Bloom's taxonomy level"));
        SCORING_CATEGORIES.put("topic_relevance", new ScoringCategory("topic_relevance", 4,
                "How well the activity covers the preferred computational thinking topics"));
        SCORING_CATEGORIES.put("duration_fit", new ScoringCategory("duration_fit", 3,
                "How well the total duration (activities + breaks) matches the target duration"));
        SCORING_CATEGORIES.put("series_cohesion", new ScoringCategory("series_cohesion", 3,
                "How well activities in a series work together (topic overlap + Bloom progression)"));
    }

    private final List<String> priorityCategories;

    public ScoringEngine() {
        this.priorityCategories = new ArrayList<>();
    }

    public ScoringEngine(List<String> priorityCategories) {
        this.priorityCategories = priorityCategories != null ? priorityCategories : new ArrayList<>();
    }

    public static Map<String, ScoringCategory> getScoringCategories() {
        return new HashMap<>(SCORING_CATEGORIES);
    }

    public ScoreResponse scoreActivity(Activity activity, SearchCriteria criteria) {
        Map<String, CategoryScoreResponse> categoryScores = new HashMap<>();

        categoryScores.put("age_appropriateness", scoreAgeAppropriateness(activity, criteria));
        categoryScores.put("bloom_level_match", scoreBloomLevelMatch(activity, criteria));
        categoryScores.put("topic_relevance", scoreTopicRelevance(activity, criteria));
        categoryScores.put("duration_fit", scoreDurationFit(Arrays.asList(activity), criteria));

        int totalScore = calculateWeightedTotal(categoryScores);

        return new ScoreResponse(totalScore, categoryScores, false, 1);
    }

    public ScoreResponse scoreSequence(List<Activity> activities, SearchCriteria criteria) {
        Map<String, CategoryScoreResponse> categoryScores = new HashMap<>();

        // Calculate average of individual scores
        List<ScoreResponse> activityScores = activities.stream()
                .map(activity -> scoreActivity(activity, criteria))
                .collect(Collectors.toList());

        Map<String, CategoryScoreResponse> avgIndividualScores = calculateAverageIndividualScores(activityScores);
        categoryScores.putAll(avgIndividualScores);

        // Add series-specific scores
        categoryScores.put("series_cohesion", scoreSeriesCohesion(activities));
        categoryScores.put("duration_fit", scoreDurationFit(activities, criteria));

        int totalScore = calculateWeightedTotal(categoryScores);

        return new ScoreResponse(totalScore, categoryScores, true, activities.size());
    }

    private CategoryScoreResponse scoreAgeAppropriateness(Activity activity, SearchCriteria criteria) {
        ScoringCategory category = SCORING_CATEGORIES.get("age_appropriateness");
        Integer targetAge = criteria.getTargetAge();

        if (targetAge == null || activity.getAgeMin() == null || activity.getAgeMax() == null) {
            return createCategoryScore(category, 0);
        }

        double rawScore = 0.0;
        if (activity.getAgeMin() <= targetAge && targetAge <= activity.getAgeMax()) {
            rawScore = 100.0;
        } else {
            int distance = targetAge < activity.getAgeMin() ? activity.getAgeMin() - targetAge
                    : targetAge - activity.getAgeMax();
            if (distance <= AGE_MAX_DISTANCE) {
                rawScore = ((double) (AGE_MAX_DISTANCE - distance) / AGE_MAX_DISTANCE) * 100.0;
            }
        }

        return createCategoryScore(category, (int) rawScore);
    }

    private CategoryScoreResponse scoreBloomLevelMatch(Activity activity, SearchCriteria criteria) {
        ScoringCategory category = SCORING_CATEGORIES.get("bloom_level_match");
        List<BloomLevel> targetBloomLevels = criteria.getBloomLevels();

        if (targetBloomLevels == null || targetBloomLevels.isEmpty() || activity.getBloomLevel() == null) {
            return createCategoryScore(category, 0);
        }

        String activityBloom = capitalize(activity.getBloomLevel().name());
        double bestRawScore = 0.0;

        for (BloomLevel targetBloom : targetBloomLevels) {
            String targetBloomStr = capitalize(targetBloom.name());

            if (targetBloomStr.equals(activityBloom)) {
                bestRawScore = Math.max(bestRawScore, 100.0);
            } else if (BLOOM_ORDER.contains(targetBloomStr) && BLOOM_ORDER.contains(activityBloom)) {
                int targetIdx = BLOOM_ORDER.indexOf(targetBloomStr);
                int activityIdx = BLOOM_ORDER.indexOf(activityBloom);
                int distance = Math.abs(targetIdx - activityIdx);

                if (distance == BLOOM_ADJACENT_LEVELS) {
                    bestRawScore = Math.max(bestRawScore, 50.0);
                }
            }
        }

        return createCategoryScore(category, (int) bestRawScore);
    }

    private CategoryScoreResponse scoreTopicRelevance(Activity activity, SearchCriteria criteria) {
        ScoringCategory category = SCORING_CATEGORIES.get("topic_relevance");
        List<String> preferredTopics = criteria.getPreferredTopics();

        if (preferredTopics == null || preferredTopics.isEmpty()) {
            return createCategoryScore(category, 0);
        }

        List<String> activityTopics = activity.getTopics();
        if (activityTopics == null || activityTopics.isEmpty()) {
            return createCategoryScore(category, 0);
        }

        Set<String> preferredSet = preferredTopics.stream()
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        Set<String> activitySet = activityTopics.stream()
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Set<String> intersection = new HashSet<>(preferredSet);
        intersection.retainAll(activitySet);

        double rawScore = preferredSet.isEmpty() ? 0.0 : ((double) intersection.size() / preferredSet.size()) * 100.0;

        return createCategoryScore(category, (int) rawScore);
    }

    private CategoryScoreResponse scoreDurationFit(List<Activity> activities, SearchCriteria criteria) {
        ScoringCategory category = SCORING_CATEGORIES.get("duration_fit");
        Integer targetDuration = criteria.getTargetDuration();

        if (targetDuration == null) {
            return createCategoryScore(category, 0);
        }

        int totalDuration = 0;
        for (int i = 0; i < activities.size(); i++) {
            Activity activity = activities.get(i);
            if (activity.getDurationMinMinutes() != null) {
                int durationMax = activity.getDurationMaxMinutes() != null ? activity.getDurationMaxMinutes()
                        : activity.getDurationMinMinutes();
                totalDuration += (activity.getDurationMinMinutes() + durationMax) / 2;
            }

            // Skip break for last activity
            boolean isLast = (i == activities.size() - 1);
            if (!isLast && activity.getBreakAfter() != null) {
                totalDuration += activity.getBreakAfter().getDuration();
            }
        }

        if (totalDuration == 0) {
            return createCategoryScore(category, 0);
        }

        double rawScore;
        if (totalDuration == targetDuration) {
            rawScore = 100.0;
        } else if (totalDuration < targetDuration) {
            double shortfallRatio = (double) (targetDuration - totalDuration) / targetDuration;
            rawScore = shortfallRatio <= 0.5 ? (1 - shortfallRatio) * 100.0 : 0.0;
        } else {
            double excessRatio = (double) (totalDuration - targetDuration) / targetDuration;
            rawScore = excessRatio <= 0.5 ? (1 - excessRatio) * 100.0 : 0.0;
        }

        return createCategoryScore(category, (int) rawScore);
    }

    private CategoryScoreResponse scoreSeriesCohesion(List<Activity> activities) {
        ScoringCategory category = SCORING_CATEGORIES.get("series_cohesion");

        if (activities.size() == 1) {
            return createCategoryScore(category, category.getImpact() * 20); // 3 * 20 = 60
        }

        // Topic overlap score
        double topicOverlapScore = 0;
        for (int i = 0; i < activities.size() - 1; i++) {
            Set<String> currentTopics = activities.get(i).getTopics() != null ? activities.get(i).getTopics().stream()
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet()) : new HashSet<>();
            Set<String> nextTopics = activities.get(i + 1).getTopics() != null
                    ? activities.get(i + 1).getTopics().stream()
                            .filter(Objects::nonNull)
                            .map(String::toLowerCase)
                            .collect(Collectors.toSet())
                    : new HashSet<>();

            if (!currentTopics.isEmpty() && !nextTopics.isEmpty()) {
                Set<String> union = new HashSet<>(currentTopics);
                union.addAll(nextTopics);
                Set<String> intersection = new HashSet<>(currentTopics);
                intersection.retainAll(nextTopics);

                double overlap = union.isEmpty() ? 0 : (double) intersection.size() / union.size();
                topicOverlapScore += overlap;
            }
        }

        if (activities.size() > 1) {
            topicOverlapScore = (topicOverlapScore / (activities.size() - 1)) * 50;
        }

        // Bloom progression score
        double bloomProgressionScore = 0;
        if (activities.size() > 1) {
            List<Integer> bloomIndices = new ArrayList<>();
            for (Activity activity : activities) {
                String bloomStr = capitalize(activity.getBloomLevel().name());
                int idx = BLOOM_ORDER.indexOf(bloomStr);
                bloomIndices.add(idx >= 0 ? idx : 0);
            }

            boolean isProgressive = true;
            for (int i = 0; i < bloomIndices.size() - 1; i++) {
                if (bloomIndices.get(i) > bloomIndices.get(i + 1)) {
                    isProgressive = false;
                    break;
                }
            }
            bloomProgressionScore = isProgressive ? 50 : 25;
        }

        int cohesionScore = Math.min((int) (topicOverlapScore + bloomProgressionScore), 100);
        return createCategoryScore(category, Math.max(0, Math.min(cohesionScore, 100)));
    }

    private Map<String, CategoryScoreResponse> calculateAverageIndividualScores(List<ScoreResponse> activityScores) {
        if (activityScores.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, CategoryScoreResponse> avgScores = new HashMap<>();
        Set<String> categoryNames = activityScores.get(0).getCategoryScores().keySet();

        for (String categoryName : categoryNames) {
            int totalScore = 0;
            for (ScoreResponse score : activityScores) {
                totalScore += score.getCategoryScores().get(categoryName).getScore();
            }
            int avgScore = totalScore / activityScores.size();

            ScoringCategory category = SCORING_CATEGORIES.get(categoryName);
            avgScores.put(categoryName, createCategoryScore(category, avgScore));
        }

        return avgScores;
    }

    private int calculateWeightedTotal(Map<String, CategoryScoreResponse> categoryScores) {
        if (categoryScores.isEmpty()) {
            return 0;
        }

        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (CategoryScoreResponse categoryScore : categoryScores.values()) {
            double weight = categoryScore.getImpact();
            weightedSum += categoryScore.getScore() * weight;
            totalWeight += weight;
        }

        if (totalWeight == 0) {
            return 0;
        }

        double weightedAverage = weightedSum / totalWeight;
        return Math.max(0, Math.min((int) weightedAverage, 100));
    }

    private CategoryScoreResponse createCategoryScore(ScoringCategory category, int score) {
        double priorityMultiplier = 1.0;
        boolean isPriority = false;

        if (priorityCategories.contains(category.getName())) {
            priorityMultiplier = PRIORITY_CATEGORY_MULTIPLIER;
            isPriority = true;
        }

        return new CategoryScoreResponse(
                category.getName(),
                Math.max(0, Math.min(score, 100)),
                category.getImpact(),
                priorityMultiplier,
                isPriority);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    public static class ScoringCategory {
        private final String name;
        private final int impact;
        private final String description;

        public ScoringCategory(String name, int impact, String description) {
            this.name = name;
            this.impact = impact;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public int getImpact() {
            return impact;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class SearchCriteria {
        private String name;
        private Integer targetAge;
        private List<ActivityFormat> formats;
        private List<BloomLevel> bloomLevels;
        private Integer targetDuration;
        private List<ActivityResource> availableResources;
        private List<String> preferredTopics;

        public SearchCriteria() {
        }

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getTargetAge() {
            return targetAge;
        }

        public void setTargetAge(Integer targetAge) {
            this.targetAge = targetAge;
        }

        public List<ActivityFormat> getFormats() {
            return formats;
        }

        public void setFormats(List<ActivityFormat> formats) {
            this.formats = formats;
        }

        public List<BloomLevel> getBloomLevels() {
            return bloomLevels;
        }

        public void setBloomLevels(List<BloomLevel> bloomLevels) {
            this.bloomLevels = bloomLevels;
        }

        public Integer getTargetDuration() {
            return targetDuration;
        }

        public void setTargetDuration(Integer targetDuration) {
            this.targetDuration = targetDuration;
        }

        public List<ActivityResource> getAvailableResources() {
            return availableResources;
        }

        public void setAvailableResources(List<ActivityResource> availableResources) {
            this.availableResources = availableResources;
        }

        public List<String> getPreferredTopics() {
            return preferredTopics;
        }

        public void setPreferredTopics(List<String> preferredTopics) {
            this.preferredTopics = preferredTopics;
        }
    }
}
