package com.eaglepoint.workforce.enums;

public enum ImportStatus {
    QUEUED("Queued"),
    VALIDATING("Validating"),
    IMPORTING("Importing"),
    COMPLETED("Completed"),
    COMPLETED_WITH_ERRORS("Completed with Errors"),
    FAILED("Failed"),
    DUPLICATE("Duplicate File");

    private final String displayName;

    ImportStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
