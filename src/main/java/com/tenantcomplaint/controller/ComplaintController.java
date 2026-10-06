package com.tenantcomplaint.controller;

import com.tenantcomplaint.dto.ComplaintSubmitDTO;
import com.tenantcomplaint.dto.TrackComplaintDTO;
import com.tenantcomplaint.entity.Complaint;
import com.tenantcomplaint.entity.ComplaintCategory;
import com.tenantcomplaint.entity.Priority;
import com.tenantcomplaint.exception.ComplaintNotFoundException;
import com.tenantcomplaint.service.ComplaintService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Handles tenant-facing complaint operations.
 */
@Controller
@RequestMapping("/complaints")
public class ComplaintController {

    private static final Logger log = LoggerFactory.getLogger(ComplaintController.class);

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    /** Show complaint submission form */
    @GetMapping("/submit")
    public String showSubmitForm(Model model) {
        model.addAttribute("complaintDTO", new ComplaintSubmitDTO());
        model.addAttribute("categories", ComplaintCategory.values());
        model.addAttribute("priorities", Priority.values());
        return "complaints/submit";
    }

    /** Process complaint submission */
    @PostMapping("/submit")
    public String submitComplaint(@Valid @ModelAttribute("complaintDTO") ComplaintSubmitDTO dto,
                                   BindingResult bindingResult,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", ComplaintCategory.values());
            model.addAttribute("priorities", Priority.values());
            return "complaints/submit";
        }

        try {
            Complaint complaint = complaintService.submitComplaint(dto);
            redirectAttributes.addFlashAttribute("complaint", complaint);
            return "redirect:/complaints/success/" + complaint.getReferenceId();
        } catch (Exception e) {
            log.error("Error submitting complaint", e);
            model.addAttribute("error", "Failed to submit complaint: " + e.getMessage());
            model.addAttribute("categories", ComplaintCategory.values());
            model.addAttribute("priorities", Priority.values());
            return "complaints/submit";
        }
    }

    /** Show success page after submission */
    @GetMapping("/success/{referenceId}")
    public String showSuccess(@PathVariable String referenceId, Model model) {
        try {
            // Try to find by referenceId and any email (for display purposes after redirect)
            if (!model.containsAttribute("complaint")) {
                // Fetch from DB since flash attribute might be gone on refresh
                List<Complaint> all = complaintService.getAllComplaints();
                Complaint complaint = all.stream()
                        .filter(c -> c.getReferenceId().equals(referenceId))
                        .findFirst()
                        .orElseThrow(() -> new ComplaintNotFoundException("Complaint not found"));
                model.addAttribute("complaint", complaint);
            }
        } catch (ComplaintNotFoundException e) {
            return "redirect:/complaints/submit";
        }
        return "complaints/success";
    }

    /** Show complaint tracking form */
    @GetMapping("/track")
    public String showTrackForm(Model model) {
        model.addAttribute("trackDTO", new TrackComplaintDTO());
        return "complaints/track";
    }

    /** Process complaint tracking */
    @PostMapping("/track")
    public String trackComplaint(@Valid @ModelAttribute("trackDTO") TrackComplaintDTO dto,
                                  BindingResult bindingResult,
                                  Model model) {
        if (bindingResult.hasErrors()) {
            return "complaints/track";
        }

        try {
            Complaint complaint = complaintService.trackComplaint(dto.getReferenceId(), dto.getEmail());
            model.addAttribute("complaint", complaint);
            model.addAttribute("statusHistory", complaintService.getStatusHistory(complaint.getId()));
            return "complaints/status";
        } catch (ComplaintNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            return "complaints/track";
        }
    }
}
