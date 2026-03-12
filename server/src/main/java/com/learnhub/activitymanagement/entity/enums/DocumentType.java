package com.learnhub.activitymanagement.entity.enums;

public enum DocumentType {

	SOURCE_PDF("source_pdf");

	private final String value;

	DocumentType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static DocumentType fromValue(String value) {
		for (DocumentType type : values()) {
			if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown document type: " + value);
	}
}
