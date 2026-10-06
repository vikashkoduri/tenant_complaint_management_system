package com.tenantcomplaint.service;

import com.tenantcomplaint.dto.ComplaintSubmitDTO;
import com.tenantcomplaint.dto.ComplaintUpdateDTO;
import com.tenantcomplaint.entity.*;
import com.tenantcomplaint.exception.ComplaintNotFoundException;
import com.tenantcomplaint.exception.InvalidStatusTransitionException;
import com.tenantcomplaint.repository.ComplaintDocumentRepository;
import com.tenantcomplaint.repository.ComplaintRepository;
import com.tenantcomplaint.repository.StatusHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Core business logic for complaint management.
 */
@Service
public class ComplaintService {

    private static final Logger log = LoggerFactory.getLogger(ComplaintService.class);

    // Allowed status transitions
    private static final Map<ComplaintStatus, Set<ComplaintStatus>> VALID_TRANSITIONS = Map.of(
            ComplaintStatus.SUBMITTED, Set.of(ComplaintStatus.UNDER_REVIEW),
            ComplaintStatus.UNDER_REVIEW, Set.of(ComplaintStatus.APPROVED, ComplaintStatus.REJECTED, ComplaintStatus.ACTION_REQUIRED),
            ComplaintStatus.ACTION_REQUIRED, Set.of(ComplaintStatus.UNDER_REVIEW, ComplaintStatus.APPROVED, ComplaintStatus.REJECTED)
    );

    private final ComplaintRepository complaintRepository;
    private final ComplaintDocumentRepository documentRepository;
    private final StatusHistoryRepository historyRepository;
    private final FileStorageService fileStorageService;

    public ComplaintService(ComplaintRepository complaintRepository,
                           ComplaintDocumentRepository documentRepository,
                           StatusHistoryRepository historyRepository,
                           FileStorageService fileStorageService) {
        this.complaintRepository = complaintRepository;
        this.documentRepository = documentRepository;
        this.historyRepository = historyRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Submit a new complaint.
     */
    @Transactional
    public Complaint submitComplaint(ComplaintSubmitDTO dto) {
        Complaint complaint = new Complaint();
        complaint.setTenantName(dto.getTenantName().trim());
        complaint.setEmail(dto.getEmail().trim().toLowerCase());
        complaint.setPhoneNumber(dto.getPhoneNumber().trim());
        complaint.setPropertyInfo(dto.getPropertyInfo().trim());
        complaint.setCategory(dto.getCategory());
        complaint.setTitle(dto.getTitle().trim());
        complaint.setDescription(dto.getDescription().trim());
        complaint.setPriority(dto.getPriority());
        complaint.setStatus(ComplaintStatus.SUBMITTED);
        complaint.setReferenceId(generateReferenceId());

        // Save complaint first to get the ID
        complaint = complaintRepository.save(complaint);

        // Handle file upload if present
        MultipartFile file = dto.getDocument();
        if (file != null && !file.isEmpty()) {
            String storedFilename = fileStorageService.storeFile(file);

            ComplaintDocument document = new ComplaintDocument();
            document.setOriginalFilename(file.getOriginalFilename());
            document.setStoredFilename(storedFilename);
            document.setContentType(file.getContentType());
            document.setFileSize(file.getSize());
            document.setComplaint(complaint);
            documentRepository.save(document);
            complaint.setDocument(document);
        }

        // Create initial status history entry
        addStatusHistory(complaint, null, ComplaintStatus.SUBMITTED, "Complaint submitted", "SYSTEM");

        log.info("Complaint submitted: {} by {}", complaint.getReferenceId(), complaint.getTenantName());
        return complaint;
    }

    /**
     * Track a complaint by reference ID and email.
     */
    public Complaint trackComplaint(String referenceId, String email) {
        return complaintRepository.findByReferenceIdAndEmail(referenceId.trim().toUpperCase(), email.trim().toLowerCase())
                .orElseThrow(() -> new ComplaintNotFoundException(
                        "No complaint found with reference ID: " + referenceId + " and the provided email"));
    }

    /**
     * Get complaint by ID (for reviewer).
     */
    public Complaint getComplaintById(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ComplaintNotFoundException("Complaint not found with ID: " + id));
    }

    /**
     * Get all complaints (for reviewer dashboard).
     */
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    /**
     * Search complaints by name or reference ID.
     */
    public List<Complaint> searchComplaints(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllComplaints();
        }
        return complaintRepository.findByTenantNameContainingIgnoreCaseOrReferenceIdContainingIgnoreCase(
                query.trim(), query.trim());
    }

    /**
     * Filter complaints by status.
     */
    public List<Complaint> filterByStatus(ComplaintStatus status) {
        if (status == null) {
            return getAllComplaints();
        }
        return complaintRepository.findByStatus(status);
    }

    /**
     * Update complaint status (reviewer action).
     */
    @Transactional
    public Complaint updateComplaintStatus(Long complaintId, ComplaintUpdateDTO dto, String reviewerEmail) {
        Complaint complaint = getComplaintById(complaintId);
        ComplaintStatus oldStatus = complaint.getStatus();
        ComplaintStatus newStatus = dto.getStatus();

        // Validate status transition
        validateStatusTransition(oldStatus, newStatus);

        complaint.setStatus(newStatus);
        if (dto.getRemarks() != null && !dto.getRemarks().trim().isEmpty()) {
            complaint.setReviewerRemarks(dto.getRemarks().trim());
        }

        complaint = complaintRepository.save(complaint);

        // Record status change history
        addStatusHistory(complaint, oldStatus, newStatus, dto.getRemarks(), reviewerEmail);

        log.info("Complaint {} status changed from {} to {} by {}",
                complaint.getReferenceId(), oldStatus, newStatus, reviewerEmail);
        return complaint;
    }

    /**
     * Get status history for a complaint.
     */
    public List<StatusHistory> getStatusHistory(Long complaintId) {
        return historyRepository.findByComplaintIdOrderByChangedAtDesc(complaintId);
    }

    /**
     * Get dashboard statistics.
     */
    public Map<String, Long> getDashboardStats() {
        return Map.of(
                "total", complaintRepository.count(),
                "submitted", complaintRepository.countByStatus(ComplaintStatus.SUBMITTED),
                "underReview", complaintRepository.countByStatus(ComplaintStatus.UNDER_REVIEW),
                "approved", complaintRepository.countByStatus(ComplaintStatus.APPROVED),
                "rejected", complaintRepository.countByStatus(ComplaintStatus.REJECTED),
                "actionRequired", complaintRepository.countByStatus(ComplaintStatus.ACTION_REQUIRED)
        );
    }

    private void validateStatusTransition(ComplaintStatus from, ComplaintStatus to) {
        if (from == to) {
            throw new InvalidStatusTransitionException("Complaint is already in " + from.getDisplayName() + " status");
        }
        Set<ComplaintStatus> allowed = VALID_TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new InvalidStatusTransitionException(
                    "Cannot change status from " + from.getDisplayName() + " to " + to.getDisplayName());
        }
    }

    private void addStatusHistory(Complaint complaint, ComplaintStatus oldStatus,
                                  ComplaintStatus newStatus, String remarks, String changedBy) {
        StatusHistory history = new StatusHistory();
        history.setComplaint(complaint);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setRemarks(remarks);
        history.setChangedBy(changedBy);
        historyRepository.save(history);
    }

    private String generateReferenceId() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = complaintRepository.count() + 1;
        return String.format("COMP-%s-%05d", datePart, count);
    }
}
