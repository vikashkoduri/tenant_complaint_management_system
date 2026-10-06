package com.tenantcomplaint.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for complaint tracking by tenant.
 */
public class TrackComplaintDTO {

    @NotBlank(message = "Complaint reference ID is required")
    private String referenceId;

    @NotBlank(message = "Email is required for verification")
    private String email;

    // Getters and Setters

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
