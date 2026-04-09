package com.eaglepoint.workforce.enums;

public enum PaymentChannel {
    CASH("Cash"),
    CHECK("Check"),
    MANUAL_CARD("Manual Card Entry");

    private final String displayName;

    PaymentChannel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
