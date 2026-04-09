package com.eaglepoint.workforce.enums;

public enum CollectorStatus {
    AVAILABLE("Available"),
    ON_JOB("On Job"),
    OFF_DUTY("Off Duty"),
    INACTIVE("Inactive");

    private final String displayName;

    CollectorStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
