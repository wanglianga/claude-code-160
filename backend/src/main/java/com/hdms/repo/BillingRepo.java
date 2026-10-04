package com.hdms.repo;

import com.hdms.model.BillingRecord;
import com.hdms.model.enums.BillingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingRepo extends JpaRepository<BillingRecord, Long> {
    Optional<BillingRecord> findByTreatmentId(Long treatmentId);
    List<BillingRecord> findByStatusOrderByCreatedAtDesc(BillingStatus status);
    List<BillingRecord> findTop200ByOrderByCreatedAtDesc();
}
