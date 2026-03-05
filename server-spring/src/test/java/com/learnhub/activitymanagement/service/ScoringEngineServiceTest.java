package com.learnhub.activitymanagement.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.learnhub.activitymanagement.dto.response.CategoryScoreResponse;
import com.learnhub.activitymanagement.dto.response.ScoreResponse;
import com.learnhub.activitymanagement.entity.Activity;
import com.learnhub.activitymanagement.entity.enums.ActivityFormat;
import com.learnhub.activitymanagement.entity.enums.BloomLevel;
import com.learnhub.activitymanagement.service.ScoringEngineService.SearchCriteria;
import java.util.*;
import org.junit.jupiter.api.Test;

class ScoringEngineServiceTest {

	private Activity createSampleActivity() {
		Activity activity = new Activity();
		activity.setName("Test Activity");
		activity.setDescription("A test activity for scoring");
		activity.setAgeMin(10);
		activity.setAgeMax(14);
		activity.setFormat(ActivityFormat.UNPLUGGED);
		activity.setBloomLevel(BloomLevel.APPLY);
		activity.setDurationMinMinutes(30);
		activity.setDurationMaxMinutes(45);
		activity.setTopics(Arrays.asList("algorithms", "patterns"));
		return activity;
	}

	private SearchCriteria createSampleCriteria() {
		SearchCriteria criteria = new SearchCriteria();
		criteria.setTargetAge(12);
		criteria.setBloomLevels(Arrays.asList(BloomLevel.APPLY));
		criteria.setTargetDuration(30);
		criteria.setPreferredTopics(Arrays.asList("algorithms"));
		return criteria;
	}

	@Test
	void scoringEngineInitializationWithoutPriorities() {
		ScoringEngineService engine = new ScoringEngineService();
		ScoreResponse score = engine.scoreActivity(createSampleActivity(), createSampleCriteria());

		assertThat(score).isNotNull();
		assertThat(score.getTotalScore()).isBetween(0, 100);
		assertThat(score.getActivityCount()).isEqualTo(1);
		assertThat(score.isSequence()).isFalse();
		assertThat(score.getCategoryScores()).hasSize(4);

		// Without priority categories, all should have multiplier 1.0
		for (CategoryScoreResponse catScore : score.getCategoryScores().values()) {
			assertThat(catScore.getPriorityMultiplier()).isEqualTo(1.0);
			assertThat(catScore.isPriority()).isFalse();
		}
	}

	@Test
	void scoringEngineWithPriorityCategories() {
		List<String> priorities = Arrays.asList("age_appropriateness", "topic_relevance");
		ScoringEngineService engine = new ScoringEngineService(priorities);
		ScoreResponse score = engine.scoreActivity(createSampleActivity(), createSampleCriteria());

		assertThat(score).isNotNull();

		// Check that priority categories have multipliers applied
		CategoryScoreResponse ageScore = score.getCategoryScores().get("age_appropriateness");
		assertThat(ageScore.getPriorityMultiplier()).isEqualTo(2.0);
		assertThat(ageScore.isPriority()).isTrue();

		CategoryScoreResponse topicScore = score.getCategoryScores().get("topic_relevance");
		assertThat(topicScore.getPriorityMultiplier()).isEqualTo(2.0);
		assertThat(topicScore.isPriority()).isTrue();

		// Non-priority categories should have multiplier 1.0
		CategoryScoreResponse bloomScore = score.getCategoryScores().get("bloom_level_match");
		assertThat(bloomScore.getPriorityMultiplier()).isEqualTo(1.0);
		assertThat(bloomScore.isPriority()).isFalse();

		CategoryScoreResponse durationScore = score.getCategoryScores().get("duration_fit");
		assertThat(durationScore.getPriorityMultiplier()).isEqualTo(1.0);
		assertThat(durationScore.isPriority()).isFalse();
	}

	@Test
	void seriesCohesionNeverHasPriority() {
		// Even if "series_cohesion" were somehow passed as a priority category,
		// it should never be flagged as priority (matches Flask behavior)
		List<String> priorities = Arrays.asList("series_cohesion", "age_appropriateness");
		ScoringEngineService engine = new ScoringEngineService(priorities);

		Activity activity1 = createSampleActivity();
		activity1.setBloomLevel(BloomLevel.REMEMBER);
		Activity activity2 = createSampleActivity();
		activity2.setBloomLevel(BloomLevel.UNDERSTAND);

		ScoreResponse score = engine.scoreSequence(Arrays.asList(activity1, activity2), createSampleCriteria());

		CategoryScoreResponse cohesionScore = score.getCategoryScores().get("series_cohesion");
		assertThat(cohesionScore).isNotNull();
		assertThat(cohesionScore.getPriorityMultiplier()).isEqualTo(1.0);
		assertThat(cohesionScore.isPriority()).isFalse();

		// But age_appropriateness should still be a priority
		CategoryScoreResponse ageScore = score.getCategoryScores().get("age_appropriateness");
		assertThat(ageScore.getPriorityMultiplier()).isEqualTo(2.0);
		assertThat(ageScore.isPriority()).isTrue();
	}

	@Test
	void seriesCohesionSingleActivityNeverHasPriority() {
		List<String> priorities = Arrays.asList("series_cohesion");
		ScoringEngineService engine = new ScoringEngineService(priorities);

		ScoreResponse score = engine.scoreSequence(Arrays.asList(createSampleActivity()), createSampleCriteria());

		CategoryScoreResponse cohesionScore = score.getCategoryScores().get("series_cohesion");
		assertThat(cohesionScore).isNotNull();
		assertThat(cohesionScore.getPriorityMultiplier()).isEqualTo(1.0);
		assertThat(cohesionScore.isPriority()).isFalse();
	}

	@Test
	void scoreActivityWithoutDuration() {
		ScoringEngineService engine = new ScoringEngineService();
		ScoreResponse score = engine.scoreActivityWithoutDuration(createSampleActivity(), createSampleCriteria());

		assertThat(score.getCategoryScores()).hasSize(3);
		assertThat(score.getCategoryScores()).containsKeys("age_appropriateness", "bloom_level_match",
				"topic_relevance");
		assertThat(score.getCategoryScores()).doesNotContainKey("duration_fit");
	}

	@Test
	void durationFitWithPriority() {
		List<String> priorities = Arrays.asList("duration_fit");
		ScoringEngineService engine = new ScoringEngineService(priorities);
		ScoreResponse score = engine.scoreActivity(createSampleActivity(), createSampleCriteria());

		CategoryScoreResponse durationScore = score.getCategoryScores().get("duration_fit");
		assertThat(durationScore.getPriorityMultiplier()).isEqualTo(2.0);
		assertThat(durationScore.isPriority()).isTrue();
	}

	@Test
	void bloomLevelMatchWithPriority() {
		List<String> priorities = Arrays.asList("bloom_level_match");
		ScoringEngineService engine = new ScoringEngineService(priorities);
		ScoreResponse score = engine.scoreActivity(createSampleActivity(), createSampleCriteria());

		CategoryScoreResponse bloomScore = score.getCategoryScores().get("bloom_level_match");
		assertThat(bloomScore.getPriorityMultiplier()).isEqualTo(2.0);
		assertThat(bloomScore.isPriority()).isTrue();
	}

	@Test
	void allFourPriorityCategories() {
		List<String> priorities = Arrays.asList("age_appropriateness", "bloom_level_match", "topic_relevance",
				"duration_fit");
		ScoringEngineService engine = new ScoringEngineService(priorities);
		ScoreResponse score = engine.scoreActivity(createSampleActivity(), createSampleCriteria());

		// All four categories should be priorities
		for (Map.Entry<String, CategoryScoreResponse> entry : score.getCategoryScores().entrySet()) {
			assertThat(entry.getValue().getPriorityMultiplier()).isEqualTo(2.0);
			assertThat(entry.getValue().isPriority()).isTrue();
		}
	}

	@Test
	void sequenceWithPriorityCategoriesAveragedScores() {
		List<String> priorities = Arrays.asList("age_appropriateness", "topic_relevance");
		ScoringEngineService engine = new ScoringEngineService(priorities);

		Activity activity1 = createSampleActivity();
		activity1.setBloomLevel(BloomLevel.REMEMBER);
		Activity activity2 = createSampleActivity();
		activity2.setBloomLevel(BloomLevel.UNDERSTAND);

		ScoreResponse score = engine.scoreSequence(Arrays.asList(activity1, activity2), createSampleCriteria());

		assertThat(score.isSequence()).isTrue();
		assertThat(score.getActivityCount()).isEqualTo(2);

		// Averaged individual scores should still reflect priority
		CategoryScoreResponse ageScore = score.getCategoryScores().get("age_appropriateness");
		assertThat(ageScore.getPriorityMultiplier()).isEqualTo(2.0);
		assertThat(ageScore.isPriority()).isTrue();

		CategoryScoreResponse topicScore = score.getCategoryScores().get("topic_relevance");
		assertThat(topicScore.getPriorityMultiplier()).isEqualTo(2.0);
		assertThat(topicScore.isPriority()).isTrue();

		// Non-priority category
		CategoryScoreResponse bloomScore = score.getCategoryScores().get("bloom_level_match");
		assertThat(bloomScore.getPriorityMultiplier()).isEqualTo(1.0);
		assertThat(bloomScore.isPriority()).isFalse();
	}
}
