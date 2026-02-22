package com.learnhub.activitymanagement.entity.enums;

public enum ActivityResource {
    COMPUTERS("computers"),
    TABLETS("tablets"),
    HANDOUTS("handouts"),
    BLOCKS("blocks"),
    ELECTRONICS("electronics"),
    STATIONERY("stationery");

    private final String value;

    ActivityResource(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ActivityResource fromValue(String value) {
        for (ActivityResource resource : values()) {
            if (resource.value.equalsIgnoreCase(value)) {
                return resource;
            }
        }
        throw new IllegalArgumentException("Unknown activity resource: " + value);
    }
}
