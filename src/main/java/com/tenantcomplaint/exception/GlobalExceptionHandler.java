package com.tenantcomplaint.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Global exception handler for the application.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ComplaintNotFoundException.class)
    public ModelAndView handleComplaintNotFound(ComplaintNotFoundException ex) {
        log.warn("Complaint not found: {}", ex.getMessage());
        ModelAndView mav = new ModelAndView("error/not-found");
        mav.addObject("errorMessage", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(FileUploadException.class)
    public String handleFileUpload(FileUploadException ex, RedirectAttributes redirectAttributes) {
        log.error("File upload error: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("fileError", ex.getMessage());
        return "redirect:/complaints/submit";
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public String handleInvalidTransition(InvalidStatusTransitionException ex,
                                          RedirectAttributes redirectAttributes) {
        log.warn("Invalid status transition: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/reviewer/dashboard";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(MaxUploadSizeExceededException ex,
                                      RedirectAttributes redirectAttributes) {
        log.warn("File too large: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("fileError", "File size exceeds the maximum limit of 5MB");
        return "redirect:/complaints/submit";
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        ModelAndView mav = new ModelAndView("error/general");
        mav.addObject("errorMessage", "An unexpected error occurred. Please try again later.");
        return mav;
    }
}
