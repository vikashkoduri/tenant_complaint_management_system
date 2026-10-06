package com.tenantcomplaint.service;

import com.tenantcomplaint.dto.ComplaintSubmitDTO;
import com.tenantcomplaint.dto.ComplaintUpdateDTO;
import com.tenantcomplaint.entity.*;
import com.tenantcomplaint.exception.ComplaintNotFoundException;
import com.tenantcomplaint.exception.InvalidStatusTransitionException;
import com.tenantcomplaint.repository.ComplaintDocumentRepository;
import com.tenantcomplaint.repository.ComplaintRepository;
import com.tenantcomplaint.repository.StatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ComplaintService.
 */
@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private ComplaintDocumentRepository documentRepository;

    @Mock
    private StatusHistoryRepository historyRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ComplaintService complaintService;

    private Complaint sampleComplaint;

    @BeforeEach
    void setUp() {
        sampleComplaint = new Complaint();
        sampleComplaint.setId(1L);
        sampleComplaint.setReferenceId("COMP-20261006-00001");
        sampleComplaint.setTenantName("John Doe");
        sampleComplaint.setEmail("john@example.com");
        sampleComplaint.setPhoneNumber("9876543210");
        sampleComplaint.setPropertyInfo("Flat 302, Block A");
        sampleComplaint.setCategory(ComplaintCategory.WATER_LEAKAGE);
        sampleComplaint.setTitle("Water leak in bathroom");
        sampleComplaint.setDescription("There is a constant water leak in the bathroom ceiling causing damage");
        sampleComplaint.setPriority(Priority.HIGH);
        sampleComplaint.setStatus(ComplaintStatus.SUBMITTED);
        sampleComplaint.setCreatedAt(LocalDateTime.now());
        sampleComplaint.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Submit complaint successfully")
    void testSubmitComplaint() {
        ComplaintSubmitDTO dto = new ComplaintSubmitDTO();
        dto.setTenantName("John Doe");
        dto.setEmail("john@example.com");
        dto.setPhoneNumber("9876543210");
        dto.setPropertyInfo("Flat 302, Block A");
        dto.setCategory(ComplaintCategory.WATER_LEAKAGE);
        dto.setTitle("Water leak in bathroom");
        dto.setDescription("There is a constant water leak in the bathroom ceiling causing damage");
        dto.setPriority(Priority.HIGH);

        when(complaintRepository.save(any(Complaint.class))).thenReturn(sampleComplaint);
        when(complaintRepository.count()).thenReturn(0L);
        when(historyRepository.save(any(StatusHistory.class))).thenReturn(new StatusHistory());

        Complaint result = complaintService.submitComplaint(dto);

        assertNotNull(result);
        assertEquals("John Doe", result.getTenantName());
        assertEquals(ComplaintStatus.SUBMITTED, result.getStatus());
        verify(complaintRepository, times(1)).save(any(Complaint.class));
        verify(historyRepository, times(1)).save(any(StatusHistory.class));
    }

    @Test
    @DisplayName("Track complaint by reference ID and email")
    void testTrackComplaint() {
        when(complaintRepository.findByReferenceIdAndEmail("COMP-20261006-00001", "john@example.com"))
                .thenReturn(Optional.of(sampleComplaint));

        Complaint result = complaintService.trackComplaint("COMP-20261006-00001", "john@example.com");

        assertNotNull(result);
        assertEquals("COMP-20261006-00001", result.getReferenceId());
    }

    @Test
    @DisplayName("Track complaint with invalid reference ID throws exception")
    void testTrackComplaintNotFound() {
        when(complaintRepository.findByReferenceIdAndEmail("INVALID-ID", "john@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(ComplaintNotFoundException.class,
                () -> complaintService.trackComplaint("INVALID-ID", "john@example.com"));
    }

    @Test
    @DisplayName("Update complaint status from SUBMITTED to UNDER_REVIEW")
    void testUpdateStatusValid() {
        sampleComplaint.setStatus(ComplaintStatus.SUBMITTED);
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(sampleComplaint));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(sampleComplaint);
        when(historyRepository.save(any(StatusHistory.class))).thenReturn(new StatusHistory());

        ComplaintUpdateDTO dto = new ComplaintUpdateDTO();
        dto.setStatus(ComplaintStatus.UNDER_REVIEW);
        dto.setRemarks("Taking this complaint for review");

        Complaint result = complaintService.updateComplaintStatus(1L, dto, "admin@tenant.com");

        assertEquals(ComplaintStatus.UNDER_REVIEW, result.getStatus());
        verify(historyRepository, times(1)).save(any(StatusHistory.class));
    }

    @Test
    @DisplayName("Invalid status transition from SUBMITTED to APPROVED throws exception")
    void testInvalidStatusTransition() {
        sampleComplaint.setStatus(ComplaintStatus.SUBMITTED);
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(sampleComplaint));

        ComplaintUpdateDTO dto = new ComplaintUpdateDTO();
        dto.setStatus(ComplaintStatus.APPROVED);

        assertThrows(InvalidStatusTransitionException.class,
                () -> complaintService.updateComplaintStatus(1L, dto, "admin@tenant.com"));
    }

    @Test
    @DisplayName("Get all complaints returns list")
    void testGetAllComplaints() {
        when(complaintRepository.findAll()).thenReturn(List.of(sampleComplaint));

        List<Complaint> result = complaintService.getAllComplaints();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Get complaint by ID returns complaint")
    void testGetComplaintById() {
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(sampleComplaint));

        Complaint result = complaintService.getComplaintById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("Get complaint by invalid ID throws exception")
    void testGetComplaintByIdNotFound() {
        when(complaintRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ComplaintNotFoundException.class,
                () -> complaintService.getComplaintById(999L));
    }

    @Test
    @DisplayName("Dashboard stats returns correct counts")
    void testGetDashboardStats() {
        when(complaintRepository.count()).thenReturn(5L);
        when(complaintRepository.countByStatus(ComplaintStatus.SUBMITTED)).thenReturn(2L);
        when(complaintRepository.countByStatus(ComplaintStatus.UNDER_REVIEW)).thenReturn(1L);
        when(complaintRepository.countByStatus(ComplaintStatus.APPROVED)).thenReturn(1L);
        when(complaintRepository.countByStatus(ComplaintStatus.REJECTED)).thenReturn(1L);
        when(complaintRepository.countByStatus(ComplaintStatus.ACTION_REQUIRED)).thenReturn(0L);

        var stats = complaintService.getDashboardStats();

        assertEquals(5L, stats.get("total"));
        assertEquals(2L, stats.get("submitted"));
    }
}
