package com.hdms.repo;

import com.hdms.model.MonitoringEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonitoringRepo extends JpaRepository<MonitoringEntry, Long> {
    List<MonitoringEntry> findByTreatmentIdOrderByRecordedAtAsc(Long treatmentId);
}
