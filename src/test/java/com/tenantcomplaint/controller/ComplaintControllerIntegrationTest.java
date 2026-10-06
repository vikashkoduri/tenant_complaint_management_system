package com.tenantcomplaint.controller;

import com.tenantcomplaint.entity.ComplaintCategory;
import com.tenantcomplaint.entity.Priority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for web controllers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComplaintControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Home page loads successfully")
    void testHomePage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"));
    }

    @Test
    @DisplayName("Submit form page loads")
    void testSubmitFormPage() throws Exception {
        mockMvc.perform(get("/complaints/submit"))
                .andExpect(status().isOk())
                .andExpect(view().name("complaints/submit"))
                .andExpect(model().attributeExists("complaintDTO", "categories", "priorities"));
    }

    @Test
    @DisplayName("Track complaint page loads")
    void testTrackPage() throws Exception {
        mockMvc.perform(get("/complaints/track"))
                .andExpect(status().isOk())
                .andExpect(view().name("complaints/track"));
    }

    @Test
    @DisplayName("Submit valid complaint redirects to success")
    void testSubmitValidComplaint() throws Exception {
        mockMvc.perform(post("/complaints/submit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("tenantName", "John Doe")
                        .param("email", "john@example.com")
                        .param("phoneNumber", "9876543210")
                        .param("propertyInfo", "Flat 302, Block A")
                        .param("category", ComplaintCategory.WATER_LEAKAGE.name())
                        .param("title", "Water leak in bathroom")
                        .param("description", "There is a constant water leak in the bathroom ceiling causing damage to walls")
                        .param("priority", Priority.HIGH.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/complaints/success/*"));
    }

    @Test
    @DisplayName("Submit invalid complaint shows validation errors")
    void testSubmitInvalidComplaint() throws Exception {
        mockMvc.perform(post("/complaints/submit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("tenantName", "")
                        .param("email", "invalid-email")
                        .param("phoneNumber", "")
                        .param("propertyInfo", "")
                        .param("title", "")
                        .param("description", "short")
                        .param("priority", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("complaints/submit"))
                .andExpect(model().hasErrors());
    }

    @Test
    @DisplayName("Login page loads")
    void testLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @DisplayName("Reviewer dashboard requires authentication")
    void testDashboardRequiresAuth() throws Exception {
        mockMvc.perform(get("/reviewer/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin@tenant.com", roles = {"REVIEWER"})
    @DisplayName("Authenticated reviewer can access dashboard")
    void testDashboardAccess() throws Exception {
        mockMvc.perform(get("/reviewer/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("reviewer/dashboard"))
                .andExpect(model().attributeExists("complaints", "stats"));
    }

    @Test
    @DisplayName("Health endpoint is accessible")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
