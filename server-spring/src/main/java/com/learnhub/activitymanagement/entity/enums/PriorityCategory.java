package com.learnhub.activitymanagement.entity.enums;

/**
 * Enum for categories that have priority multipliers - used by API to indicate
 * high priority categories. Matches Flask's PriorityCategory enum.
 */
public enum PriorityCategory {

	AGE_APPROPRIATENESS("age_appropriateness"),
	TOPIC_RELEVANCE("topic_relevance"),
	DURATION_FIT("duration_fit"),
	BLOOM_LEVEL_MATCH("bloom_level_match"),
	SERIES_DURATION_FIT("series_duration_fit");

	private final String value;

	PriorityCategory(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static PriorityCategory fromValue(String value) {
		for (PriorityCategory category : values()) {
			if (category.value.equalsIgnoreCase(value)) {
				return category;
			}
		}
		throw new IllegalArgumentException("Unknown priority category: " + value);
	}
}
