package com.learnhub.activitymanagement.entity.enums;

public enum ActivityTopic {
    DECOMPOSITION("decomposition"),
    PATTERNS("patterns"),
    ABSTRACTION("abstraction"),
    ALGORITHMS("algorithms");

    private final String value;

    ActivityTopic(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ActivityTopic fromValue(String value) {
        for (ActivityTopic topic : values()) {
            if (topic.value.equalsIgnoreCase(value)) {
                return topic;
            }
        }
        throw new IllegalArgumentException("Unknown activity topic: " + value);
    }
}
