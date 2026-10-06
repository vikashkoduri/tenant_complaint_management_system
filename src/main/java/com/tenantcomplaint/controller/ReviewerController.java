package com.tenantcomplaint.controller;

import com.tenantcomplaint.dto.ComplaintUpdateDTO;
import com.tenantcomplaint.entity.Complaint;
import com.tenantcomplaint.entity.ComplaintStatus;
import com.tenantcomplaint.service.ComplaintService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Reviewer/Admin dashboard controller - requires REVIEWER role.
 */
@Controller
@RequestMapping("/reviewer")
public class ReviewerController {

    private static final Logger log = LoggerFactory.getLogger(ReviewerController.class);

    private final ComplaintService complaintService;

    public ReviewerController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    /** Reviewer dashboard with all complaints */
    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String search,
                           @RequestParam(required = false) ComplaintStatus status,
                           Model model) {
        List<Complaint> complaints;

        if (search != null && !search.trim().isEmpty()) {
            complaints = complaintService.searchComplaints(search);
            model.addAttribute("search", search);
        } else if (status != null) {
            complaints = complaintService.filterByStatus(status);
            model.addAttribute("selectedStatus", status);
        } else {
            complaints = complaintService.getAllComplaints();
        }

        model.addAttribute("complaints", complaints);
        model.addAttribute("statuses", ComplaintStatus.values());
        model.addAttribute("stats", complaintService.getDashboardStats());
        return "reviewer/dashboard";
    }

    /** View complaint details */
    @GetMapping("/complaint/{id}")
    public String viewComplaint(@PathVariable Long id, Model model) {
        Complaint complaint = complaintService.getComplaintById(id);
        model.addAttribute("complaint", complaint);
        model.addAttribute("statusHistory", complaintService.getStatusHistory(id));
        model.addAttribute("updateDTO", new ComplaintUpdateDTO());
        model.addAttribute("statuses", ComplaintStatus.values());
        return "reviewer/complaint-detail";
    }

    /** Update complaint status */
    @PostMapping("/complaint/{id}/update")
    public String updateComplaint(@PathVariable Long id,
                                   @Valid @ModelAttribute("updateDTO") ComplaintUpdateDTO dto,
                                   BindingResult bindingResult,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        if (bindingResult.hasErrors()) {
            Complaint complaint = complaintService.getComplaintById(id);
            model.addAttribute("complaint", complaint);
            model.addAttribute("statusHistory", complaintService.getStatusHistory(id));
            model.addAttribute("statuses", ComplaintStatus.values());
            return "reviewer/complaint-detail";
        }

        try {
            String reviewerEmail = authentication.getName();
            complaintService.updateComplaintStatus(id, dto, reviewerEmail);
            redirectAttributes.addFlashAttribute("success", "Complaint status updated successfully");
        } catch (Exception e) {
            log.error("Error updating complaint {}", id, e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/reviewer/complaint/" + id;
    }
}
