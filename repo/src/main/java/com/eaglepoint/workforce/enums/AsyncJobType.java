package com.eaglepoint.workforce.enums;

public enum AsyncJobType {
    FACE_FEATURE_EXTRACTION("Face Feature Extraction"),
    FACE_VERIFICATION("Face Verification"),
    BATCH_IMPORT("Batch Import"),
    REPORT_GENERATION("Report Generation"),
    DATA_EXPORT("Data Export");

    private final String displayName;

    AsyncJobType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
