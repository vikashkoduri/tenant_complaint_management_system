package com.tenantcomplaint.dto;

import com.tenantcomplaint.entity.ComplaintCategory;
import com.tenantcomplaint.entity.Priority;
import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO for complaint submission form.
 */
public class ComplaintSubmitDTO {

    @NotBlank(message = "Tenant name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String tenantName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+]?[0-9\\s\\-()]{7,20}$", message = "Please provide a valid phone number")
    private String phoneNumber;

    @NotBlank(message = "Property/flat information is required")
    @Size(min = 2, max = 200, message = "Property info must be between 2 and 200 characters")
    private String propertyInfo;

    @NotNull(message = "Complaint category is required")
    private ComplaintCategory category;

    @NotBlank(message = "Complaint title is required")
    @Size(min = 5, max = 150, message = "Title must be between 5 and 150 characters")
    private String title;

    @NotBlank(message = "Complaint description is required")
    @Size(min = 20, max = 2000, message = "Description must be between 20 and 2000 characters")
    private String description;

    @NotNull(message = "Priority is required")
    private Priority priority;

    private MultipartFile document;

    // Getters and Setters

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getPropertyInfo() { return propertyInfo; }
    public void setPropertyInfo(String propertyInfo) { this.propertyInfo = propertyInfo; }

    public ComplaintCategory getCategory() { return category; }
    public void setCategory(ComplaintCategory category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public MultipartFile getDocument() { return document; }
    public void setDocument(MultipartFile document) { this.document = document; }
}
