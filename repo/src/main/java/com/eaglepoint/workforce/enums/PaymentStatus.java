package com.eaglepoint.workforce.enums;

public enum PaymentStatus {
    RECORDED("Recorded"),
    RECONCILED("Reconciled"),
    REFUNDED("Refunded"),
    PARTIALLY_REFUNDED("Partially Refunded"),
    VOIDED("Voided");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
