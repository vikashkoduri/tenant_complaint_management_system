package com.tenantcomplaint.dto;

import com.tenantcomplaint.entity.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for reviewer updating complaint status.
 */
public class ComplaintUpdateDTO {

    @NotNull(message = "Status is required")
    private ComplaintStatus status;

    @Size(max = 1000, message = "Remarks must not exceed 1000 characters")
    private String remarks;

    // Getters and Setters

    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
