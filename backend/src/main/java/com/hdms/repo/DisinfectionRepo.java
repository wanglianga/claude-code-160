package com.hdms.repo;

import com.hdms.model.DisinfectionRecord;
import com.hdms.model.enums.DisinfectionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface DisinfectionRepo extends JpaRepository<DisinfectionRecord, Long> {
    List<DisinfectionRecord> findByMachineIdOrderByPerformedAtDesc(Long machineId);
    List<DisinfectionRecord> findByPerformedAtBetween(LocalDateTime from, LocalDateTime to);
    List<DisinfectionRecord> findByMachineIdAndTypeAndPerformedAtBetween(Long machineId, DisinfectionType type, LocalDateTime from, LocalDateTime to);
    List<DisinfectionRecord> findTop50ByOrderByPerformedAtDesc();
}
