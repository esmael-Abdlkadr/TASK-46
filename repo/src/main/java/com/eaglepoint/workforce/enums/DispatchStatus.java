package com.eaglepoint.workforce.enums;

public enum DispatchStatus {
    PENDING("Pending"),
    OFFERED("Offered"),
    ACCEPTED("Accepted"),
    DECLINED("Declined"),
    TIMED_OUT("Timed Out"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    DispatchStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
