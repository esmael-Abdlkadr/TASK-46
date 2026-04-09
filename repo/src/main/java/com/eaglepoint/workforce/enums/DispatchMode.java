package com.eaglepoint.workforce.enums;

public enum DispatchMode {
    GRAB_ORDER("Grab Order"),
    ASSIGNED_ORDER("Assigned Order");

    private final String displayName;

    DispatchMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
