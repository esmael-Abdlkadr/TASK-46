package com.eaglepoint.workforce.enums;

public enum ReconciliationStatus {
    MATCHED("Matched"),
    UNMATCHED_PAYMENT("Unmatched Payment"),
    UNMATCHED_BANK("Unmatched Bank Entry"),
    DISCREPANCY("Amount Discrepancy"),
    RESOLVED("Resolved");

    private final String displayName;

    ReconciliationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
