package com.eaglepoint.workforce.enums;

public enum AsyncJobStatus {
    QUEUED("Queued"),
    RUNNING("Running"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELLED("Cancelled");

    private final String displayName;

    AsyncJobStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
