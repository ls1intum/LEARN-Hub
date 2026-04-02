package com.learnhub.activitymanagement.entity.enums;

public enum MarkdownType {

	ARTIKULATIONSSCHEMA("artikulationsschema"), DECKBLATT("deckblatt"), HINTERGRUNDWISSEN("hintergrundwissen"),
	UEBUNG("uebung"), UEBUNG_LOESUNG("uebung_loesung");

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
