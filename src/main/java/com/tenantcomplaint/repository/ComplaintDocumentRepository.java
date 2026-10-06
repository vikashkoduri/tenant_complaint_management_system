package com.tenantcomplaint.repository;

import com.tenantcomplaint.entity.ComplaintDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComplaintDocumentRepository extends JpaRepository<ComplaintDocument, Long> {
    Optional<ComplaintDocument> findByComplaintId(Long complaintId);
}
