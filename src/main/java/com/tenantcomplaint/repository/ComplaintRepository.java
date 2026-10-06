package com.tenantcomplaint.repository;

import com.tenantcomplaint.entity.Complaint;
import com.tenantcomplaint.entity.ComplaintCategory;
import com.tenantcomplaint.entity.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    Optional<Complaint> findByReferenceId(String referenceId);

    Optional<Complaint> findByReferenceIdAndEmail(String referenceId, String email);

    List<Complaint> findByStatus(ComplaintStatus status);

    List<Complaint> findByCategory(ComplaintCategory category);

    List<Complaint> findByTenantNameContainingIgnoreCaseOrReferenceIdContainingIgnoreCase(
            String tenantName, String referenceId);

    long countByStatus(ComplaintStatus status);
}
