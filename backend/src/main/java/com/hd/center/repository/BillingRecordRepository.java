package com.hd.center.repository;

import com.hd.center.entity.BillingRecord;
import com.hd.center.entity.Enums.BillingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingRecordRepository extends JpaRepository<BillingRecord, Long> {
    Optional<BillingRecord> findBySessionId(Long sessionId);
    List<BillingRecord> findByStatusOrderByCreatedAtDesc(BillingStatus status);
    List<BillingRecord> findTop100ByOrderByCreatedAtDesc();
}
