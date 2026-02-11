package com.learnhub.activitymanagement.entity.enums;

public enum BloomLevel {
    REMEMBER("remember"),
    UNDERSTAND("understand"),
    APPLY("apply"),
    ANALYZE("analyze"),
    EVALUATE("evaluate"),
    CREATE("create");

    private final String value;

    BloomLevel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static BloomLevel fromValue(String value) {
        for (BloomLevel level : values()) {
            if (level.value.equalsIgnoreCase(value)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown bloom level: " + value);
    }
}
