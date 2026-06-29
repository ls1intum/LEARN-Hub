package com.learnhub.activitymanagement.entity.enums;

public enum MarkdownType {

	LESSON_PLAN("lesson_plan"), COVER_SHEET("cover_sheet"), BACKGROUND_KNOWLEDGE("background_knowledge"), BOARD_IMAGE(
			"board_image"), EXERCISE("exercise"), EXERCISE_SOLUTION("exercise_solution");

	private final String value;

	MarkdownType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static MarkdownType fromValue(String value) {
		for (MarkdownType type : values()) {
			if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown markdown type: " + value);
	}
}
