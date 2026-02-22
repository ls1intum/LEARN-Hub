package com.learnhub.activitymanagement.entity.enums;

public enum EnergyLevel {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    private final String value;

    EnergyLevel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static EnergyLevel fromValue(String value) {
        for (EnergyLevel level : values()) {
            if (level.value.equalsIgnoreCase(value)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown energy level: " + value);
    }
}
