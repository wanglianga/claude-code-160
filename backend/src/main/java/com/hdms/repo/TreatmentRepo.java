package com.hdms.repo;

import com.hdms.model.TreatmentRecord;
import com.hdms.model.enums.TreatmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TreatmentRepo extends JpaRepository<TreatmentRecord, Long> {
    Optional<TreatmentRecord> findByScheduleId(Long scheduleId);
    List<TreatmentRecord> findByStatus(TreatmentStatus status);
    List<TreatmentRecord> findByScheduleScheduleDate(java.time.LocalDate date);
    List<TreatmentRecord> findByScheduleScheduleDateBetween(java.time.LocalDate from, java.time.LocalDate to);
}
