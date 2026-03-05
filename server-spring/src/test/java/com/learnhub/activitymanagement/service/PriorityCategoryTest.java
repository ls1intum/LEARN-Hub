package com.learnhub.activitymanagement.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.learnhub.activitymanagement.entity.enums.PriorityCategory;
import java.util.*;
import org.junit.jupiter.api.Test;

class PriorityCategoryTest {

	@Test
	void validPriorityCategoryValues() {
		assertThat(PriorityCategory.fromValue("age_appropriateness")).isEqualTo(PriorityCategory.AGE_APPROPRIATENESS);
		assertThat(PriorityCategory.fromValue("topic_relevance")).isEqualTo(PriorityCategory.TOPIC_RELEVANCE);
		assertThat(PriorityCategory.fromValue("duration_fit")).isEqualTo(PriorityCategory.DURATION_FIT);
		assertThat(PriorityCategory.fromValue("bloom_level_match")).isEqualTo(PriorityCategory.BLOOM_LEVEL_MATCH);
		assertThat(PriorityCategory.fromValue("series_duration_fit")).isEqualTo(PriorityCategory.SERIES_DURATION_FIT);
	}

	@Test
	void invalidPriorityCategoryThrowsException() {
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
				() -> PriorityCategory.fromValue("invalid_category"));
	}

	@Test
	void seriesCohesionIsNotAValidPriorityCategory() {
		// series_cohesion should not be a valid priority category
		// (matches Flask's PriorityCategory enum which does not include series_cohesion)
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
				() -> PriorityCategory.fromValue("series_cohesion"));
	}

	@Test
	void priorityCategoryValues() {
		assertThat(PriorityCategory.AGE_APPROPRIATENESS.getValue()).isEqualTo("age_appropriateness");
		assertThat(PriorityCategory.TOPIC_RELEVANCE.getValue()).isEqualTo("topic_relevance");
		assertThat(PriorityCategory.DURATION_FIT.getValue()).isEqualTo("duration_fit");
		assertThat(PriorityCategory.BLOOM_LEVEL_MATCH.getValue()).isEqualTo("bloom_level_match");
		assertThat(PriorityCategory.SERIES_DURATION_FIT.getValue()).isEqualTo("series_duration_fit");
	}

	@Test
	void caseInsensitiveFromValue() {
		assertThat(PriorityCategory.fromValue("Age_Appropriateness")).isEqualTo(PriorityCategory.AGE_APPROPRIATENESS);
		assertThat(PriorityCategory.fromValue("TOPIC_RELEVANCE")).isEqualTo(PriorityCategory.TOPIC_RELEVANCE);
	}
}
