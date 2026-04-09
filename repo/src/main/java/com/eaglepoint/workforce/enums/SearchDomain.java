package com.eaglepoint.workforce.enums;

public enum SearchDomain {
    CANDIDATES("Candidates"),
    COLLECTORS("Collectors"),
    SITES("Sites"),
    REQUISITIONS("Requisitions"),
    DISPATCH_ASSIGNMENTS("Dispatch Assignments"),
    DEPARTMENTS("Departments"),
    TRAINING_COURSES("Training Courses"),
    MEMBERS("Members"),
    ENTERPRISES("Enterprises"),
    RESOURCES("Resources"),
    ORDERS("Orders"),
    REDEMPTIONS("Redemption Records");

    private final String displayName;

    SearchDomain(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
