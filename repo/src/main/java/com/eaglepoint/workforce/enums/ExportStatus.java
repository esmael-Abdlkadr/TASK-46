package com.eaglepoint.workforce.enums;

public enum ExportStatus {
    QUEUED("Queued"),
    RUNNING("Running"),
    COMPLETED("Completed"),
    FAILED("Failed");

    private final String displayName;

    ExportStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
