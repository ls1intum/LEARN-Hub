package com.learnhub.activitymanagement.entity.enums;

public enum ActivityFormat {
    UNPLUGGED("unplugged"),
    DIGITAL("digital"),
    HYBRID("hybrid");

    private final String value;

    ActivityFormat(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ActivityFormat fromValue(String value) {
        for (ActivityFormat format : values()) {
            if (format.value.equalsIgnoreCase(value)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown activity format: " + value);
    }
}
