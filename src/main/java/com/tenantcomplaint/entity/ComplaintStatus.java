package com.tenantcomplaint.entity;

/**
 * Complaint status lifecycle.
 */
public enum ComplaintStatus {
    SUBMITTED("Submitted"),
    UNDER_REVIEW("Under Review"),
    ACTION_REQUIRED("Action Required"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String displayName;

    ComplaintStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
