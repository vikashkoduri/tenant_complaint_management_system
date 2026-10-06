package com.tenantcomplaint.entity;

/**
 * Complaint categories.
 */
public enum ComplaintCategory {
    WATER_LEAKAGE("Water Leakage"),
    ELECTRICITY("Electricity"),
    MAINTENANCE("Maintenance"),
    PLUMBING("Plumbing"),
    SECURITY("Security"),
    CLEANLINESS("Cleanliness"),
    OTHER("Other");

    private final String displayName;

    ComplaintCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
