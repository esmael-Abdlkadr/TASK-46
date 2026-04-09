package com.eaglepoint.workforce.enums;

public enum PipelineStage {
    SOURCED("Sourced"),
    SCREENING("Screening"),
    PHONE_INTERVIEW("Phone Interview"),
    TECHNICAL_INTERVIEW("Technical Interview"),
    ONSITE_INTERVIEW("Onsite Interview"),
    OFFER_EXTENDED("Offer Extended"),
    OFFER_ACCEPTED("Offer Accepted"),
    HIRED("Hired"),
    REJECTED("Rejected"),
    WITHDRAWN("Withdrawn");

    private final String displayName;

    PipelineStage(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
